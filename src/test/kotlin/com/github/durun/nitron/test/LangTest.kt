package com.github.durun.nitron.test

import com.github.durun.nitron.core.config.AntlrParserConfig
import com.github.durun.nitron.core.config.LangConfig
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import com.github.durun.nitron.core.parser.antlr.ParserStore
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
import kotlin.io.path.toPath


@ExperimentalPathApi
class LangTest : FreeSpec({
	val configPath = Paths.get("config/nitron.json")
    NitronConfigLoader.load(configPath).langConfig
        .filter { (_, config) -> config.parserConfig is AntlrParserConfig }
        .forEach { (lang, config) -> include(langTestFactory(lang, config)) }
})


@ExperimentalPathApi
fun langTestFactory(lang: String, config: LangConfig) = freeSpec {
	"config for $lang (${config.fileName})" - {
        val parser = ParserStore.getOrNull(config.parserConfig)
        val parserConfig = config.parserConfig as AntlrParserConfig
        "grammar files exist" {
            val files = parserConfig.grammarFilePaths + parserConfig.utilJavaFilePaths
            log { "${config.fileName}: files=$files" }
            files shouldHaveAtLeastSize 1
            files.forAll {
                it.shouldBeReadable()
            }
        }
        "defines parser settings" {
            shouldNotThrowAny {
                ParserStore.getOrThrow(config.parserConfig)
			}
		}
		"defines start rule" {
            val startRule = parserConfig.startRule
            log { "${config.fileName}: startRule=$startRule" }
            startRule shouldBeIn parser!!.antlrParser.ruleNames
        }
		"defines at least 1 extension of sourcecode" {
            val extensions = config.extensions
            log { "${config.fileName}: extensions=$extensions" }
            extensions shouldHaveAtLeastSize 1
        }
		"uses correct rule/token name" {
			val usedRules: List<String> = (
                    config.processConfig.normalizeConfig.ignoreRules +
                            config.processConfig.normalizeConfig.mapping.keys +
                            config.processConfig.normalizeConfig.indexedMapping.keys +
                            config.processConfig.splitConfig.splitRules
                    )
                .flatMap { it.split('/').filter(String::isNotEmpty) }
                .filterNot { it.contains(Regex("[^a-zA-Z]")) }
			val antlrParser = parser!!.antlrParser
			val allowedRules = antlrParser.ruleNames + antlrParser.tokenTypeMap.keys
			runCatching {
				allowedRules shouldContainAll usedRules
			}.onFailure {
                println("see: ${parserConfig.grammarFilePaths.map(Path::normalize)}")
                val errorSymbols = (usedRules - allowedRules)
                config.fileUri.toURL().readText().lineSequence().forEachIndexed { index, line ->
                    val file = config.fileUri.toPath()
                    val lineNo = index + 1
                    errorSymbols.filter { line.contains(it) }.forEach {
                        println("""$file:$lineNo: "$it" is not defined""")
                    }
                }
			}.getOrThrow()
		}
	}
}