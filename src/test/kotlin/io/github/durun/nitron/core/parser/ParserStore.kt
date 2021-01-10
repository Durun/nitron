package io.github.durun.nitron.core.parser

import io.github.durun.nitron.core.config.GrammarConfig
import io.kotest.mpp.log

object ParserStore {
	private val parsers = mutableMapOf<GrammarConfig, Result<CommonParser>>()


	private val mapping = { it: GrammarConfig ->
		runCatching {
			log("Compiling grammar: ${it.fileName}")
			val p = CommonParser(grammarFiles = it.grammarFilePaths, utilityJavaFiles = it.utilJavaFilePaths)
			log("Compiled grammar: ${it.fileName}")
			p
		}
	}

	fun getOrThrow(config: GrammarConfig): CommonParser {
		return parsers.computeIfAbsent(config, mapping).getOrThrow()
	}

	fun getOrNull(config: GrammarConfig): CommonParser? {
		return parsers.computeIfAbsent(config, mapping).getOrNull()
	}
}