package io.github.durun.nitron.core.parser.antlr

import io.github.durun.nitron.core.config.AntlrParserConfig
import io.github.durun.nitron.core.config.ParserConfig
import io.kotest.mpp.log
import java.nio.file.Path

object ParserStore {
    private val parsers = mutableMapOf<Pair<Set<Path>, Set<Path>>, GenericParser>()

    fun getOrNull(config: ParserConfig): GenericParser? = runCatching { getOrThrow(config) }.getOrNull()
    fun getOrThrow(config: ParserConfig): GenericParser = getOrThrow(config as AntlrParserConfig)
    fun getOrThrow(config: AntlrParserConfig): GenericParser =
        getOrThrow(config.grammarFilePaths, config.utilJavaFilePaths)
    fun getOrThrow(grammarFiles: Collection<Path>, utilJavaFiles: Collection<Path> = emptySet()): GenericParser {
        return synchronized(parsers) {
            parsers.computeIfAbsent(grammarFiles.toSet() to utilJavaFiles.toSet()) { (gFiles, uFiles) ->
                val parser = GenericParser.fromFiles(gFiles, uFiles)
                log { "Compiled grammar: ${parser.antlrParser.grammarFileName}" }
                parser
            }
        }
    }
}