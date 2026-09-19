package com.github.zero9178.mlirods.model

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.InputStream

/**
 * Name of the file LLVM's CMake writes the compilation commands of all TableGen files to.
 */
const val COMPILE_COMMANDS_FILE_NAME = "tablegen_compile_commands.yml"

/**
 * Compilation command of the file [filepath] with [includes] being its include paths separated by ';'.
 */
// Note: Needs to be public and mutable due to limitations in snakeyaml.
data class FileInfoDto(
    var filepath: String = "",
    var includes: String = "",
)

/**
 * Reads compilation commands in the format of [COMPILE_COMMANDS_FILE_NAME] from [inputStream].
 */
fun readCompilationCommands(inputStream: InputStream): List<FileInfoDto> =
    Yaml(Constructor(FileInfoDto::class.java, LoaderOptions())).loadAll(inputStream).filterIsInstance<FileInfoDto>()
