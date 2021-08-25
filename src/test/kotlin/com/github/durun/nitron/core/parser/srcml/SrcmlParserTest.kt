package com.github.durun.nitron.core.parser.srcml

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.visitor.flatten
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.core.spec.style.FreeSpec
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
        //println(ast.accept(AstPrintVisitor))

        val tokens = ast.flatten() as List<AstTerminalNode>
        tokens.forEach {
            println("${it.line}: ${it.token}")
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
        //println(ast.accept(AstPrintVisitor))

        val tokens = ast.flatten() as List<AstTerminalNode>
        tokens.forEach {
            println("${it.line}: ${it.token}")
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
        //println(ast.accept(AstPrintVisitor))

        val tokens = ast.flatten() as List<AstTerminalNode>
        tokens.forEach {
            println("${it.line}: ${it.token}")
        }
    }
})
