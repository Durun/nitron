package com.github.durun.nitron.core.parser.srcml

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.visitor.flatten
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Path
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class SrcmlParserTest : FreeSpec({
    "parse C#" {
        val source = """
            using System;
            public class Hello {
              public static void Main() {
                if (cond1) Console.WriteLine("hello world!");
                else if (true) n = 1;
                else c = 'a';
                return null;
              }
            }
        """.trimIndent()
        val langConfig = NitronConfigLoader.load(Path.of("config/nitron.json")).langConfig["csharp-srcml"]!!
        val parser = langConfig.parserConfig.getParser()

        val ast: AstNode
        measureTime {
            ast = parser.parse(source.reader())
        }.let { println("parse source to AST: $it") }
        println(ast)
        ast.getText().removeSpaceAndNL() shouldBe source.removeSpaceAndNL()

        val tokens = ast.flatten().shouldBeInstanceOf<List<AstTerminalNode>>()
        val lines = source.split('\n')
        tokens.forAll {
            println("${it.line}: ${it.token}")
            lines[it.line - 1] shouldContain it.token
        }
    }

    "parse C" {
        val source = """
            /* Hello World program */
            #include "stdio.h"
            main() {
              if (cond) printf("Hello World");
              else if (true) n = 1;
              else c = 'a';
              return 0;
            }
        """.trimIndent()
        val langConfig = NitronConfigLoader.load(Path.of("config/nitron.json")).langConfig["c-srcml"]!!
        val parser = langConfig.parserConfig.getParser()

        val ast: AstNode
        measureTime {
            ast = parser.parse(source.reader())
        }.let { println("parse source to AST: $it") }
        println(ast)
        ast.getText().removeSpaceAndNL() shouldBe source.removeSpaceAndNL()

        val tokens = ast.flatten().shouldBeInstanceOf<List<AstTerminalNode>>()
        val lines = source.split('\n')
        tokens.forAll {
            println("${it.line}: ${it.token}")
            lines[it.line - 1] shouldContain it.token
        }
    }

    "parse C++" {
        val source = """
            #include <iostream.h>
            main() {
                if (cond) cout << "Hello World!";
                else if (true) n = 1;
                else c = 'a';
                return 0;
            }
        """.trimIndent()
        val langConfig = NitronConfigLoader.load(Path.of("config/nitron.json")).langConfig["cpp-srcml"]!!
        val parser = langConfig.parserConfig.getParser()

        val ast: AstNode
        measureTime {
            ast = parser.parse(source.reader())
        }.let { println("parse source to AST: $it") }
        println(ast)
        ast.getText().removeSpaceAndNL() shouldBe source.removeSpaceAndNL()

        val tokens = ast.flatten().shouldBeInstanceOf<List<AstTerminalNode>>()
        val lines = source.split('\n')
        tokens.forAll {
            println("${it.line}: ${it.token}")
            lines[it.line - 1] shouldContain it.token
        }
    }
})

private fun String.removeSpaceAndNL() = this.filterNot { it in " \n" }