import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease.Channel
import org.jetbrains.intellij.platform.gradle.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import java.util.*

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.grammarKit)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// The JVM toolchain defaults to the Java version required by the targeted IntelliJ Platform.
kotlin {
    compilerOptions {
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
    }
}

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }

    implementation("org.yaml:snakeyaml:2.7")
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

    }

    publishing {
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        failureLevel = listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES,
            VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
            VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES,
        )

        ides {
            select {
                types = providers.gradleProperty("verifyIDEs").map {
                    it.split(',').filter { it.isNotEmpty() }.map {
                        IntelliJPlatformType.fromCode(it)
                    }
                }
                channels = listOf(
                    Channel.EAP,
                    Channel.BETA,
                    Channel.RELEASE,
                    Channel.RC
                )
            }
        }
    }
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

val extraSourceDirs = mutableListOf("src/main/parser")
tasks {
    test {
        // Load only this plugin and what it needs into the test IDE rather than every plugin bundled with CLion. The
        // CLion language plugin in particular reconfigures the IDE and reloads its project model from a background
        // process, which changes settings and project roots in the middle of tests.
        systemProperty("idea.load.plugins.id", "com.github.zero9178.mlirods,intellij.libraries.misc.plugin")
    }

    fun generateParserTask(suffix: String = "", config: GenerateParserTask.() -> Unit = {}) =
        register<GenerateParserTask>(
            "generateParser${
                suffix.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            }") {
            targetRootOutputDir.set(file("src/main/parser"))
            sourceFile.set(file("src/main/kotlin/com/github/zero9178/mlirods/language/TableGen.bnf"))
            // Only files below these paths count as task outputs. Without them the task declares no outputs at all,
            // is always considered up-to-date and restores nothing from the build cache. The PSI root covers the whole
            // generated package rather than just the psi subpackage so that the element type holder class next to
            // the parser is tracked as well.
            pathToParser.set("com/github/zero9178/mlirods/language/generated/TableGenParser.java")
            pathToPsiRoot.set("com/github/zero9178/mlirods/language/generated")
            config()
        }

    val initial = generateParserTask("initial")
    val generateParser = generateParserTask("final") {
        dependsOn(compileKotlin)
        classpath(compileKotlin.get().outputs)
    }

    generateLexer {
        sourceFile.set(file("src/main/kotlin/com/github/zero9178/mlirods/language/TableGen.flex"))
        extraSourceDirs += "src/main/lexer"
        targetRootOutputDir.set(file(extraSourceDirs.last()))
        // The generated class is the only declared task output; see the parser tasks above.
        pathToClass.set("com/github/zero9178/mlirods/language/generated/TableGenLexer.java")

        dependsOn(initial)
    }
    val stringLexer = register<GenerateLexerTask>("generateStringLexer") {
        sourceFile.set(file("src/main/kotlin/com/github/zero9178/mlirods/highlighting/TableGenString.flex"))
        extraSourceDirs += "src/main/stringLexer"
        targetRootOutputDir.set(file(extraSourceDirs.last()))
        pathToClass.set("com/github/zero9178/mlirods/highlighting/generated/TableGenStringLexer.java")
    }

    compileKotlin {
        dependsOn(generateLexer, stringLexer, initial)
    }
    compileJava {
        dependsOn(generateLexer, stringLexer, generateParser)
    }
}

sourceSets {
    main {
        java {
            srcDir(extraSourceDirs)
        }
        kotlin {
            srcDir(extraSourceDirs)
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }

        register("runIU") {
            type = IntelliJPlatformType.IntellijIdeaUltimate
            version = providers.gradleProperty("platformVersion")
            sandboxDirectory = intellijPlatform.sandboxContainer.dir("runIU-sandbox")
        }
        register("runCL") {
            type = IntelliJPlatformType.CLion
            version = providers.gradleProperty("platformVersion")
            sandboxDirectory = intellijPlatform.sandboxContainer.dir("runCL-sandbox")
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf("-Xmx8192m")
                }
            }
        }
    }
}
