package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.model.CompilationCommandsState
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenCompilationCommandsProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.util.messages.impl.subscribeAsFlow
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
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

private fun CPPEnvironment.toLocalVFS(path: String): VirtualFile? {
    val instance = VirtualFileManager.getInstance()
    return toLocalPathAndCopyIfDoesNotExist(
        path
    )?.let { localPath ->
        instance.findFileByNioPath(Path(localPath))
    }
}

private const val COMPILE_COMMANDS_FILE_NAME = "tablegen_compile_commands.yml"


class CMakeTableGenCompilationCommandsProvider : TableGenCompilationCommandsProvider {

    private enum class VFSChange {
        IncludeChanges, CompileCommandChange,
    }

    override fun getCompilationCommandsFlow(project: Project): Flow<CompilationCommandsState> {
        // React to VCS changes that might create, move or delete one of the files returned by the flow.
        val vfsChangeFlow = project.messageBus.subscribeAsFlow(VirtualFileManager.VFS_CHANGES) {
            send(VFSChange.IncludeChanges)
            send(VFSChange.CompileCommandChange)
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events.forEach {
                        when (it) {
                            is VFileCreateEvent -> if (it.isDirectory) trySend(VFSChange.IncludeChanges)
                            else if (it.childName == COMPILE_COMMANDS_FILE_NAME) trySend(VFSChange.CompileCommandChange)

                            is VFileMoveEvent -> if (it.file.isDirectory) trySend(VFSChange.IncludeChanges)
                            else if (it.file.name == COMPILE_COMMANDS_FILE_NAME) trySend(VFSChange.CompileCommandChange)

                            is VFileDeleteEvent -> if (it.file.isDirectory) trySend(VFSChange.IncludeChanges)
                            else if (it.file.name == COMPILE_COMMANDS_FILE_NAME) trySend(VFSChange.CompileCommandChange)

                            is VFileContentChangeEvent -> {
                                if (it.file.name == COMPILE_COMMANDS_FILE_NAME) trySend(VFSChange.CompileCommandChange)
                            }
                        }
                    }
                }
            }
        }
        val compileCommandsChangeFlow = vfsChangeFlow.filter { it == VFSChange.CompileCommandChange }
        val includeChangesFlow = vfsChangeFlow.filter { it == VFSChange.IncludeChanges }

        return project.service<CMakeActiveProfileService>().profileFlow.filterNotNull()
            .combine(compileCommandsChangeFlow) { profile, _ ->
                profile.generationDir.resolve(COMPILE_COMMANDS_FILE_NAME) to profile.environment
            }.transform { (file, env) ->
                if (env == null) return@transform

                try {
                    val result = file.inputStream().use { inputStream ->
                        Yaml(Constructor(FileInfoDto::class.java, LoaderOptions())).loadAll(inputStream)
                            .filterIsInstance<FileInfoDto>()
                    }
                    emit(result to env)
                } catch (_: FileNotFoundException) {
                    // Swallow completely.
                    emit(emptyList<FileInfoDto>() to env)
                } catch (e: Throwable) {
                    // Rethrow cancellations.
                    currentCoroutineContext().ensureActive()
                    // Otherwise just warn.
                    LOG.warn(e)
                }
            }.distinctUntilChanged().combine(includeChangesFlow) { (dtos, env), _ ->
                val map = dtos.flatMap { dto ->
                    val virtualFile = env.toLocalVFS(dto.filepath)
                    if (virtualFile == null) {
                        LOG.warn("failed to find virtual file for ${dto.filepath}")
                        return@flatMap emptyList()
                    }

                    listOf(
                        virtualFile to IncludePaths(
                            dto.includes.split(
                                ';'
                            ).flatMap {
                                val virtualFile = env.toLocalVFS(it)
                                if (virtualFile == null) {
                                    LOG.warn("failed to find virtual file for $it")
                                    return@flatMap emptyList()
                                }
                                listOf(virtualFile)
                            })
                    )
                }.associate { it }
                CompilationCommandsState(map)
            }
    }
}
