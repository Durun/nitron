package io.github.durun.nitron.core.parser

import io.github.durun.nitron.core.config.GrammarConfig
import io.kotest.mpp.log
import java.nio.file.Path

object ParserStore {
	private val parsers = mutableMapOf<Pair<Set<Path>, Set<Path>?>, Result<GenericParser>>()

	private val mapping: (Pair<Set<Path>, Set<Path>>) -> Result<GenericParser> = { (grammarFiles, utilityJavaFiles) ->
		runCatching {
			val p = GenericParser.fromFiles(grammarFiles, utilityJavaFiles)
			log("Compiled grammar: ${p.antlrParser.grammarFileName}")
			p
		}
	}

	fun getOrThrow(config: GrammarConfig): GenericParser = getOrThrow(config.grammarFilePaths, config.utilJavaFilePaths)
	fun getOrThrow(grammarFiles: Collection<Path>, utilJavaFiles: Collection<Path> = emptySet()): GenericParser {
		val key = grammarFiles.toSet() to utilJavaFiles.toSet()
		return synchronized(parsers) {
			parsers.computeIfAbsent(key) { mapping(key) }
		}.getOrThrow()
	}

	fun getOrNull(config: GrammarConfig): GenericParser? = getOrNull(config.grammarFilePaths, config.utilJavaFilePaths)
	fun getOrNull(grammarFiles: Collection<Path>, utilJavaFiles: Collection<Path> = emptySet()): GenericParser? {
		val key = grammarFiles.toSet() to utilJavaFiles.toSet()
		return synchronized(parsers) {
			parsers.computeIfAbsent(key) { mapping(key) }
		}.getOrNull()
	}
}