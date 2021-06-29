package io.github.durun.nitron.core.parser.antlr

import io.github.durun.nitron.core.config.AntlrParserConfig
import io.github.durun.nitron.core.config.ParserConfig
import io.kotest.mpp.log
import java.nio.file.Path

object ParserStore {
    private val parsers = mutableMapOf<Pair<Set<Path>, Set<Path>?>, Result<GenericParser>>()

    private val mapping: (Pair<Set<Path>, Set<Path>>) -> Result<GenericParser> = { (grammarFiles, utilityJavaFiles) ->
        runCatching {
            val p = GenericParser.fromFiles(grammarFiles, utilityJavaFiles)
            log { "Compiled grammar: ${p.antlrParser.grammarFileName}" }
            p
        }
    }

    fun getOrThrow(config: ParserConfig): GenericParser = getOrThrow(config as AntlrParserConfig)
    fun getOrThrow(config: AntlrParserConfig): GenericParser =
        getOrThrow(config.grammarFilePaths, config.utilJavaFilePaths)

    fun getOrThrow(grammarFiles: Collection<Path>, utilJavaFiles: Collection<Path> = emptySet()): GenericParser {
        val key = grammarFiles.toSet() to utilJavaFiles.toSet()
        return synchronized(parsers) {
            parsers.computeIfAbsent(key) { mapping(key) }
        }.getOrThrow()
    }

    fun getOrNull(config: ParserConfig): GenericParser? = getOrNull(config as AntlrParserConfig)
    fun getOrNull(config: AntlrParserConfig): GenericParser? =
        getOrNull(config.grammarFilePaths, config.utilJavaFilePaths)

    fun getOrNull(grammarFiles: Collection<Path>, utilJavaFiles: Collection<Path> = emptySet()): GenericParser? {
        val key = grammarFiles.toSet() to utilJavaFiles.toSet()
        return synchronized(parsers) {
            parsers.computeIfAbsent(key) { mapping(key) }
        }.getOrNull()
    }
}