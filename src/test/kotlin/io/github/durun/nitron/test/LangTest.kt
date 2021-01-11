package io.github.durun.nitron.test

import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.core.parser.ParserStore
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.freeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.paths.shouldBeReadable
import io.kotest.mpp.log
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedReader


@ExperimentalPathApi
class LangTest : FreeSpec({
	val configPath = Paths.get("config/nitron.json")
	NitronConfigLoader.load(configPath).langConfig.forEach { (lang, config) ->
		include(langTestFactory(lang, config))
	}
})


@ExperimentalPathApi
fun langTestFactory(lang: String, config: LangConfig) = freeSpec {
	"config for $lang (${config.fileName})" - {
		val parser = ParserStore.getOrNull(config.grammar)
		"grammar files exist" {
			val files = config.grammar.grammarFilePaths + config.grammar.utilJavaFilePaths
			log("${config.fileName}: files=$files")
			files shouldHaveAtLeastSize 1
			files.forAll {
				it.shouldBeReadable()
			}
		}
		"defines parser settings" {
			shouldNotThrowAny {
				ParserStore.getOrThrow(config.grammar)
			}
		}
		"defines start rule" {
			val startRule = config.grammar.startRule
			log("${config.fileName}: startRule=$startRule")
			startRule shouldBeIn parser!!.antlrParser.ruleNames
		}
		"defines at least 1 extension of sourcecode" {
			val extensions = config.extensions
			log("${config.fileName}: extensions=$extensions")
			extensions shouldHaveAtLeastSize 1
		}
		"uses correct rule/token name" {
			val usedRules = config.process.normalizeConfig.ignoreRules +
					config.process.normalizeConfig.nonNumberedRuleMap.flatMap { it.key } +
					config.process.normalizeConfig.numberedRuleMap.flatMap { it.key } +
					config.process.splitConfig.splitRules
			val antlrParser = parser!!.antlrParser
			val allowedRules = antlrParser.ruleNames + antlrParser.tokenTypeMap.keys
			runCatching {
				allowedRules shouldContainAll usedRules
			}.onFailure {
				println("see: ${config.grammar.grammarFilePaths.map(Path::normalize)}")
				val errorSymbols = (usedRules - allowedRules)
				config.filePath.bufferedReader().lineSequence().forEachIndexed { index, line ->
					val file = config.filePath
					val lineNo = index + 1
					errorSymbols.filter { line.contains(it) }.forEach {
						println("""$file:$lineNo: "$it" is not defined""")
					}
				}
			}.getOrThrow()
		}
	}
}