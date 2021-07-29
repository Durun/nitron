package com.github.durun.nitron.core.parser.antlr

import com.github.durun.nitron.core.config.AntlrParserConfig
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.freeSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import java.nio.file.Paths

class GenericParserTest : FreeSpec({
	val configPath = Paths.get("config/nitron.json")
    val config = NitronConfigLoader.load(configPath)

    include(
        tests(
            "javascript",
            config.langConfig["javascript"]!!.parserConfig as AntlrParserConfig,
            src = """console.log("Hello");"""
        )
    )

    include(
        tests(
            "csharp",
            config.langConfig["csharp"]!!.parserConfig as AntlrParserConfig,
            src = """
				class HelloClass {
				    void hello() { }
				}
			""".trimIndent()
        )
    )
})

private fun tests(name: String, config: AntlrParserConfig, src: String) = freeSpec {
    val parser = ParserStore.getOrThrow(config)
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