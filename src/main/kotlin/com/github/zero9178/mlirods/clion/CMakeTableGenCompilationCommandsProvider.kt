package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.model.TableGenCompilationCommandsProvider
import com.github.zero9178.mlirods.model.CompilationCommandsState
import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.util.messages.impl.subscribeAsFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.FileNotFoundException
import kotlin.io.path.Path

// Note: Needs to be public due to limitations in snakeyaml.
data class FileInfoDto(
    var filepath: String = "",
    var includes: String = "",
)

private val LOG = logger<CMakeTableGenCompilationCommandsProvider>()

class CMakeTableGenCompilationCommandsProvider : TableGenCompilationCommandsProvider {

    override fun getCompilationCommandsFlow(project: Project): Flow<CompilationCommandsState> {
        // React to VCS changes that might create, move or delete one of the files returned by the flow.
        val vfsChangeFlow = project.messageBus.subscribeAsFlow(VirtualFileManager.VFS_CHANGES) {
            send(Unit)
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    if (events.any {
                            it is VFileCreateEvent || it is VFileMoveEvent || it is VFileDeleteEvent
                        }) trySend(Unit)
                }
            }
        }

        return project.service<CMakeActiveProfileService>().profileFlow.filterNotNull().map {
            it.generationDir.resolve("tablegen_compile_commands.yml")
        }.transform { file ->
            try {
                val result = file.inputStream().use { inputStream ->
                    Yaml(Constructor(FileInfoDto::class.java, LoaderOptions())).loadAll(inputStream)
                        .filterIsInstance<FileInfoDto>()
                }
                emit(result)
            } catch (_: FileNotFoundException) {
                // Swallow completely.
            } catch (e: Throwable) {
                // Rethrow cancellations.
                currentCoroutineContext().ensureActive()
                // Otherwise just warn.
                LOG.warn(e)
            }
        }.combine(vfsChangeFlow) { dtos, _ ->
            val instance = VirtualFileManager.getInstance()
            val map = dtos.flatMap {
                val virtualFile = instance.findFileByNioPath(Path(it.filepath))
                if (virtualFile == null) {
                    LOG.warn("failed to find virtual file for ${it.filepath}")
                    return@flatMap emptyList()
                }

                listOf(
                    virtualFile to IncludePaths(
                        it.includes.split(
                            ';'
                        ).map { it -> Path(it) })
                )
            }.associate { it }
            CompilationCommandsState(map)
        }
    }
}
