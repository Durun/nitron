package com.github.durun.nitron.validation

import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.matchers.shouldBe
import java.nio.file.Paths


private val configPath = Paths.get("config/nitron.json")
private val config = NitronConfigLoader.load(configPath).langConfig["python3"] ?: throw Exception()

private val processor = CodeProcessor(config)

fun main() = testReportAsMarkDown {
    "package" - {
        "ignore import" {
            "import foo".normalized() shouldBe emptyList()
            "import foo as bar".normalized() shouldBe emptyList()
            "from foo import bar as baz".normalized() shouldBe emptyList()
        }
        "import *" {
            "from foo import *".normalized() shouldBe emptyList()
        }
    }
    "class" - {
        "class declaration" {
            """
            class Hoge:
                def method(self):
                    call()
            """.trimIndent()
                .normalized() shouldBe listOf("class Hoge :", "{", "def method ( V0 ) :", "{", "call ( )", "}", "}")
        }

        "extends" {
            """
            class Hoge(object):
                @staticmethod
                def method():
                    call()
            """.trimIndent()
                .normalized() shouldBe listOf(
                "class Hoge ( object ) :",
                "{",
                "@ staticmethod def method ( ) :",
                "{",
                "call ( )",
                "}",
                "}"
            )
        }
    }
    "literals" - {
        "string" {
            """"hello"""".normalized() shouldBe listOf(""""S"""")
            """'hello'""".normalized() shouldBe listOf(""""S"""")
            "\"\"\"docstring\"\"\"".normalized() shouldBe listOf(""""S"""")
        }
        "number" {
            "123".normalized() shouldBe listOf("N")
        }
        "floating point" {
            "3.14".normalized() shouldBe listOf("N")
        }
    }
    "variables" - {
        "assignment" {
            "x = f()".normalized() shouldBe listOf("V0 = f ( )")
        }
        "multi assignment" {
            "a, b = f()".normalized() shouldBe listOf("V0 , V1 = f ( )")
        }
        "declaration (typed)" {
            "a: int = 1".normalized() shouldBe listOf("V0 : int = N")
        }
        "argument" {
            "f(a1)".normalized() shouldBe listOf("f ( V0 )")
        }
        "named argument" {
            "f(a=1)".normalized() shouldBe listOf("f ( a = N )")
        }
        "correct variable index" {
            "f(a0, a1, a0, a2)".normalized() shouldBe listOf("f ( V0 , V1 , V0 , V2 )")
        }
        "expression" {
            "f((x+y)*z)".normalized() shouldBe listOf("f ( ( V0 + V1 ) * V2 )")
            "f(x+f())".normalized() shouldBe listOf("f ( V0 + f ( ) )")
        }
        "member field access" {
            "f(m.name)".normalized() shouldBe listOf("f ( V0 . name )")
            "f(m.name.length)".normalized() shouldBe listOf("f ( V0 . name . length )")
        }
        "member method access" {
            "m.toString()".normalized() shouldBe listOf("V0 . toString ( )")
            "m.name.toString()".normalized() shouldBe listOf("V0 . name . toString ( )")
        }
        "function declaration" {
            """
            def func(a, b):
                return 1
            """.trimIndent()
                .normalized() shouldBe listOf("def func ( V0 , V1 ) :", "{", "return N", "}")
        }
        "function declaration (typed)" {
            """
            def func(a: int) -> int:
                return 1
            """.trimIndent()
                .normalized() shouldBe listOf("def func ( V0 : int ) -> int :", "{", "return N", "}")
        }
    }
    "split statements" - {
        "expression statements" {
            """
            invoke1()
            invoke2()
            """.trimIndent()
                .normalized() shouldBe listOf("invoke1 ( )", "invoke2 ( )")

        }
        "try-except block" {
            """
            try:
              invoke1()
            except E as e:
              invoke2()
            """.trimIndent()
                .normalized() shouldBe listOf(
                "try :",
                "{",
                "invoke1 ( )",
                "}",
                "except E as V0 :",
                "{",
                "invoke2 ( )",
                "}"
            )
        }
        "try-except-finally block" {
            """
            try:
              invoke1()
            except E as e:
              invoke2()
            finally:
              invoke3()
            """.trimIndent()
                .normalized() shouldBe listOf(
                "try :",
                "{",
                "invoke1 ( )",
                "}",
                "except E as V0 :",
                "{",
                "invoke2 ( )",
                "}",
                "finally :",
                "{",
                "invoke3 ( )",
                "}"
            )
        }
        "while block" {
            """
            while cond:
                continue
            """.trimIndent()
                .normalized() shouldBe listOf("while V0 :", "{", "continue", "}")
        }
        "for block" {
            """
            for i in list:
                pass
            """.trimIndent()
                .normalized() shouldBe listOf("for V0 in V1 :", "{", "pass", "}")
        }
        "if block" {
            """
            if cond:
                pass
            """.trimIndent()
                .normalized() shouldBe listOf("if V0 :", "{", "pass", "}")
        }
        "if-else block" {
            """
            if cond:
                pass
            else:
                pass
            """.trimIndent()
                .normalized() shouldBe listOf("if V0 :", "{", "pass", "}", "else :", "{", "pass", "}")
        }
        "elif block" {
            """
            if cond1:
                pass
            elif cond2:
                pass
            else:
                pass
            """.trimIndent()
                .normalized() shouldBe listOf(
                "if V0 :",
                "{",
                "pass",
                "}",
                "elif V0 :",
                "{",
                "pass",
                "}",
                "else :",
                "{",
                "pass",
                "}"
            )
        }
        "return statement" {
            """
            def func():
                return hoge
            """.trimIndent()
                .normalized() shouldBe listOf("def func ( ) :", "{", "return V0", "}")
        }
        "assert statement" {
            """
            assert cond, "message"
            """.trimIndent()
                .normalized() shouldBe listOf("assert V0 , \"S\"")
        }
        "raise statement" {
            """
            raise error
            """.trimIndent()
                .normalized() shouldBe listOf("raise V0")
        }
    }
    "data" - {
        "list" {
            "[1, a]".normalized() shouldBe listOf("[ N , V0 ]")
        }
        "dict" {
            "{ n: 1, m: a }".normalized() shouldBe listOf("{ n : N , m : V0 }")
        }
        "generator" {
            "[x for x in list]".normalized() shouldBe listOf("[ V0 for V0 in V1 ]")
            "(x for x in list)".normalized() shouldBe listOf("( V0 for V0 in V1 )")
        }
        "lambda" {
            "f = lambda x: x+1".normalized() shouldBe listOf("V0 = lambda V1 : V1 + N")
        }
    }
}

private fun String.normalized(): List<String> {
    return processor.split("\n" + this + "\n")
        .mapNotNull { processor.proceess(it) }
        .onEach { println(it.accept(AstPrintVisitor)) }
        .onEach { println(it.accept(AstXmlBuildVisitor)) }
        .map { it.getText() }
}