package io.github.durun.nitron.core.parser.antlr

import io.github.durun.nitron.core.config.GrammarConfig
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.freeSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
class GenericParserTest : FreeSpec({
	val configPath = Paths.get("config/nitron.json")
    val config = NitronConfigLoader.load(configPath)

    include(
        tests(
            "javascript",
            config.langConfig["javascript"]!!.grammar,
            src = """console.log("Hello");"""
        )
    )

    include(
        tests(
            "csharp",
            config.langConfig["csharp"]!!.grammar,
            src = """
				class HelloClass {
				    void hello() { }
				}
			""".trimIndent()
        )
    )
})

@ExperimentalPathApi
private fun tests(name: String, config: GrammarConfig, src: String) = freeSpec {
	val parser by lazy {
		GenericParser.fromFiles(
				config.grammarFilePaths,
				utilityJavaFiles = config.utilJavaFilePaths
		)
	}
	name - {
		"init" {
			shouldNotThrowAny {
				println(parser)
			}
		}
		"antlrParser" {
			val antlr = shouldNotThrowAny {
				parser.antlrParser
			}
			antlr.ruleNames shouldHaveAtLeastSize 1
			antlr.tokenTypeMap.entries shouldHaveAtLeastSize 1
		}
		"parse" {
			println("parsing source: $src")
			val tree = shouldNotThrowAny {
				parser.parse(src.reader(), config.startRule)
			}
			println(tree.toInfoString(parser.antlrParser))
		}
	}
}