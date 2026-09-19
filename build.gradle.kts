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

// Integration tests drive a real, separately downloaded IDE from the outside through the Starter framework. They live
// in their own source set as they run on JUnit 5 and must not see the IDE classes the unit tests compile against.
val integrationTest: SourceSet = sourceSets.create("integrationTest")

// Plugin installed next to this plugin into the IDEs run by the integration tests. It supplies this plugin with the
// compilation commands of the project being tested, which makes the tests independent of CLion and CMake.
val integrationTestPlugin: SourceSet = sourceSets.create("integrationTestPlugin") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}
val integrationTestPluginId = "com.github.zero9178.mlirods.integrationTest"

// The tests name the plugin whose classes they call into within annotations, the arguments of which have to be
// constants. Generate the constant to not have the tests repeat the ID.
val generateIntegrationTestConstants = tasks.register("generateIntegrationTestConstants") {
    description = "Generates the constants the integration tests obtain from the build."
    val id = integrationTestPluginId
    val outputDir = layout.buildDirectory.dir("generated/integrationTest")
    inputs.property("id", id)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().file("Constants.kt").asFile.writeText(
            "package com.github.zero9178.mlirods\n\ninternal const val TEST_PLUGIN_ID = \"$id\"\n"
        )
    }
}
kotlin.sourceSets[integrationTest.name].kotlin.srcDir(generateIntegrationTestConstants)

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(libs.junit)

    // Unlike everything else in this build, the integration tests run in a JVM of their own and not within an IDE
    // supplying the Kotlin standard library.
    "integrationTestImplementation"(kotlin("stdlib"))
    "integrationTestImplementation"(libs.junitJupiter)
    "integrationTestRuntimeOnly"(libs.junitPlatformLauncher)
    // Used by the Starter framework whenever an IDE exits, yet not a dependency Gradle sees of it.
    "integrationTestRuntimeOnly"(libs.teamcityServiceMessages)
    "integrationTestImplementation"(libs.kodein)
    "integrationTestImplementation"(libs.kotlinxCoroutines)

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
        testFramework(TestFrameworkType.Starter, configurationName = "integrationTestImplementation")
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
    currentProject {
        instrumentation {
            // Kover makes 'check' run every test task it measures the coverage of. The integration tests are not to be
            // part of 'check', nor do they execute any code of the plugin within their own JVM.
            disabledForTestTasks.add("integrationTest")
        }
    }
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

// Layout of an installed plugin: a directory named after the plugin with its jars in 'lib'.
val integrationTestPluginJar = tasks.register<Jar>("integrationTestPluginJar") {
    description = "Assembles the plugin supporting the integration tests."
    archiveBaseName = "mlirods-integration-test"
    from(integrationTestPlugin.output.classesDirs)
    // Not a resource of the source set: the IntelliJ Platform Gradle Plugin places the descriptor of this plugin into
    // the resources of every source set, replacing any other.
    from("src/integrationTestPlugin/plugin.xml") {
        into("META-INF")
        expand("id" to integrationTestPluginId)
    }
}
val prepareIntegrationTestPlugin = tasks.register<Sync>("prepareIntegrationTestPlugin") {
    description = "Lays out the plugin supporting the integration tests the way an IDE installs plugins."
    from(integrationTestPluginJar)
    into(layout.buildDirectory.dir("integrationTestPlugin/mlirods-integration-test/lib"))
}

// The IDE creates its '.idea' directory within the project it opens. Open a copy to keep the repository clean and
// have every run start from the same state.
val prepareIntegrationTestProject = tasks.register<Sync>("prepareIntegrationTestProject") {
    description = "Copies the project the integration tests open."
    from("src/integrationTest/testData/llvm-project")
    into(layout.buildDirectory.dir("integrationTestProject/llvm-project"))
}

intellijPlatformTesting {
    testIdeUi {
        // Not part of 'check': the tests download the IDE they are run in.
        register("integrationTest") {
            task {
                testClassesDirs = integrationTest.output.classesDirs
                classpath = integrationTest.runtimeClasspath
                useJUnitPlatform {
                    // Brought in by the Starter framework without a version of JUnit 4 it is able to work with.
                    excludeEngines("junit-vintage")
                }

                val integrationTestDir = layout.buildDirectory.dir("integrationTest")
                systemProperty("platform.version", providers.gradleProperty("platformVersion").get())
                // Number of times the inspections are run to arrive at a stable figure for the time they take.
                systemProperty(
                    "integration.test.iterations",
                    providers.gradleProperty("integrationTestIterations").getOrElse("5")
                )
                // '-PintegrationTestHeadless=false' shows the user interface of the IDE, e.g. to debug a test.
                systemProperty(
                    "integration.test.headless",
                    providers.gradleProperty("integrationTestHeadless").getOrElse("true")
                )
                systemProperty("integration.test.dir", integrationTestDir.get().asFile.path)
                systemProperty("integration.test.report.dir", integrationTestDir.get().dir("reports").asFile.path)
                systemProperty(
                    "integration.test.plugin",
                    prepareIntegrationTestPlugin.map { it.destinationDir.parentFile.path }.get()
                )
                systemProperty("integration.test.project", prepareIntegrationTestProject.map { it.destinationDir.path }.get())
                dependsOn(prepareIntegrationTestPlugin, prepareIntegrationTestProject)
                // Written to the working directory, i.e. the root of the repository, by the Starter framework otherwise.
                systemProperty("allure.results.directory", integrationTestDir.get().dir("allure-results").asFile.path)
                testLogging.showStandardStreams = true
            }
        }
    }

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
