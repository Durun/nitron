package io.github.durun.nitron.core.parser

import io.github.durun.nitron.core.config.GrammarConfig
import io.kotest.mpp.log
import java.nio.file.Path

object ParserStore {
	private val parsers = mutableMapOf<Pair<Set<Path>, Set<Path>?>, Result<CommonParser>>()

	private val mapping: (Pair<Set<Path>, Set<Path>?>) -> Result<CommonParser> = { (grammarFiles, utilityJavaFiles) ->
		runCatching {
			val p = CommonParser(grammarFiles.toList(), utilityJavaFiles?.toList())
			log("Compiled grammar: ${p.getAntlrParser().grammarFileName}")
			p
		}
	}

	fun getOrThrow(config: GrammarConfig): CommonParser = getOrThrow(config.grammarFilePaths, config.utilJavaFilePaths)
	fun getOrThrow(grammarFiles: Collection<Path>, utilJavaFiles: Collection<Path>? = null): CommonParser {
		val key = grammarFiles.toSet() to utilJavaFiles?.toSet()
		return synchronized(parsers) {
			parsers.computeIfAbsent(key) { mapping(key) }
		}.getOrThrow()
	}

	fun getOrNull(config: GrammarConfig): CommonParser? = getOrNull(config.grammarFilePaths, config.utilJavaFilePaths)
	fun getOrNull(grammarFiles: Collection<Path>, utilJavaFiles: Collection<Path>? = null): CommonParser? {
		val key = grammarFiles.toSet() to utilJavaFiles?.toSet()
		return synchronized(parsers) {
			parsers.computeIfAbsent(key) { mapping(key) }
		}.getOrNull()
	}
}