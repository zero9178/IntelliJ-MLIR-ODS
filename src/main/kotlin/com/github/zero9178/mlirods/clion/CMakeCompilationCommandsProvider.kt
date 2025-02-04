package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.model.CompilationCommandsProvider
import com.github.zero9178.mlirods.model.CompilationCommandsState
import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

data class FileInfoDto(
    var filepath: String = "",
    var includes: String = "",
)

private val LOG = logger<CMakeCompilationCommandsProvider>()

class CMakeCompilationCommandsProvider : CompilationCommandsProvider {

    override fun getCompilationCommandsFlow(project: Project) =
        project.service<CMakeActiveProfileService>().profileFlow.filterNotNull().map {
            it.generationDir.resolve("tablegen_compile_commands.yml")
        }.transform { file ->
            try {
                val result = file.inputStream().use { inputStream ->
                    val yaml = Yaml(Constructor(FileInfoDto::class.java, LoaderOptions()))
                    val map = yaml.loadAll(inputStream).filterIsInstance<FileInfoDto>().associate {
                        it.filepath to IncludePaths(it.includes.split(';'))
                    }
                    CompilationCommandsState(map)
                }
                emit(result)
            } catch (e: Throwable) {
                // Rethrow cancellations.
                currentCoroutineContext().ensureActive()
                LOG.warn(e)
            }
        }
}