package com.github.durun.nitron.validation

import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.matchers.shouldBe
import java.nio.file.Paths


private val configPath = Paths.get("config/nitron.json")
private val config = NitronConfigLoader.load(configPath).langConfig["typescript"] ?: throw Exception()

private val processor = CodeProcessor(config)

fun main() = testReportAsMarkDown {
    "package" - {
        "ignore import" {
            """import somepackage from "somepackage";""".normalized() shouldBe emptyList()
            """import * as somepackage from "somepackage";""".normalized() shouldBe emptyList()
        }
        "ignore export" {
            "export { somepackage };".normalized() shouldBe emptyList()
        }
    }
    "literal" - {
        "string" {
            code(funcBody = """"hello"""").normalized() shouldBe normStatements(funcBody = """"S"""")
            code(funcBody = "'hello'").normalized() shouldBe normStatements(funcBody = """"S"""")
        }
        "integer" {
            code(funcBody = "123").normalized() shouldBe normStatements(funcBody = "N")
            code(funcBody = "0b1").normalized() shouldBe normStatements(funcBody = "N")
            code(funcBody = "0o1").normalized() shouldBe normStatements(funcBody = "N")
            code(funcBody = "0x1").normalized() shouldBe normStatements(funcBody = "N")
        }
        "floating point" {
            code(funcBody = "1.2").normalized() shouldBe normStatements(funcBody = "N")
        }
    }
    "variable" - {
        "assignment" {
            code(funcBody = "x = f()").normalized() shouldBe normStatements(funcBody = "V0 = f ( )")
        }
        "declaration" {
            code(funcBody = "var i: number = 1").normalized() shouldBe normStatements(funcBody = "var V0 : number = N")
            code(funcBody = "const i = 1").normalized() shouldBe normStatements(funcBody = "const V0 = N")
        }
        "argument" {
            code(funcBody = "f(a)").normalized() shouldBe normStatements(funcBody = "f ( V0 )")
        }
        "correct variable index" {
            code(funcBody = "f(a0, a1, a0, a2)").normalized() shouldBe normStatements(funcBody = "f ( V0 , V1 , V0 , V2 )")
        }
        "expression" {
            code(funcBody = "f((x+y)*z)").normalized() shouldBe normStatements(funcBody = "f ( ( V0 + V1 ) * V2 )")
            code(funcBody = "f(x+f())").normalized() shouldBe normStatements(funcBody = "f ( V0 + f ( ) )")
        }
        "member access" {
            code(funcBody = "v.m = f()").normalized() shouldBe normStatements(funcBody = "V0 . m = f ( )")
            code(funcBody = "f(v.name.length)").normalized() shouldBe normStatements(funcBody = "f ( V0 . name . length )")
            code(funcBody = "v.f()").normalized() shouldBe normStatements(funcBody = "V0 . f ( )")
        }
    }
    "split statements" - {
        "expression statements" {
            code(
                funcBody = """
                call1()
                call2()
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("call1 ( )", "call2 ( )"))
        }
        "while statement" {
            code(funcBody = "while (cond) call();").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "while ( V0 )",
                    "call ( ) ;"
                )
            )
        }
        "while block" {
            code(
                funcBody = """
                while (cond) {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("while ( V0 )", "{", "call ( )", "}"))
        }
        "do while block" {
            code(
                funcBody = """
                do {
                  i++;
                } while (cond);
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("do", "{", "V0 ++ ;", "}", "while ( V0 ) ;"))
        }
        "for block" {
            code(
                funcBody = """
                for(var i = 0; i < 2; i++) {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("for ( var V0 = N ; V0 < N ; V0 ++ )", "{", "call ( )", "}"))
        }
        "for in block" {
            code(
                funcBody = """
                for(var i in list) {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("for ( var V0 in V1 )", "{", "call ( )", "}"))
        }
        "for of block" {
            code(
                funcBody = """
                for(var i of list) {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("for ( var V0 of V1 )", "{", "call ( )", "}"))
        }
        "if statement" {
            code(
                funcBody = """
                if (cond) call();
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("if ( V0 )", "call ( ) ;"))
        }
        "if block" {
            code(
                funcBody = """
                if (cond) {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("if ( V0 )", "{", "call ( )", "}"))
        }
        "if-else statement" {
            code(
                funcBody = """
                if (cond) call();
                else call();
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("if ( V0 )", "call ( ) ;", "else", "call ( ) ;"))
        }
        "if-else block" {
            code(
                funcBody = """
                if (cond) {
                  call()
                } else {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "if ( V0 )",
                    "{",
                    "call ( )",
                    "}",
                    "else",
                    "{",
                    "call ( )",
                    "}"
                )
            )
        }
        "switch" {
            code(
                funcBody = """
                switch (x) {
                case 1:
                  call();
                  break;
                default:
                  call();
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "switch ( V0 ) {",
                    "case N :",
                    "call ( ) ;",
                    "break ;",
                    "default :",
                    "call ( ) ;",
                    "}"
                )
            )
        }
        "return statement" {
            code(funcBody = "return call();").normalized() shouldBe normStatements(funcBody = "return call ( ) ;")
        }
        "try-catch block" {
            code(
                funcBody = """
                try {
                  call();
                } catch (e) {
                  call();
                } finally {
                  call();
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "try {",
                    "call ( ) ;",
                    "}",
                    "catch ( V0 ) {",
                    "call ( ) ;",
                    "}",
                    "finally {",
                    "call ( ) ;",
                    "}"
                )
            )
        }
    }
    "data" - {
        "array" {
            code(funcBody = "a[0]").normalized() shouldBe normStatements(funcBody = "V0 [ N ]")
        }
        "closure" {
            code(funcBody = "let f = function () { return x; };").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "let V0 = function ( ) {",
                    "return V0 ;",
                    "} ;"
                )
            )
            code(funcBody = "const f = (x) => 1;").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "const V0 = ( V1 ) => N ;"
                )
            )
        }
    }
    "declaration" - {
        "function" {
            code(
                body = """
                function f(i: number): number {
                    return 0;
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                body = listOf(
                    "function f ( V0 : number ) : number {",
                    "return N ;",
                    "}"
                )
            )
        }
    }
}


private fun code(
    funcBody: String = "",
    body: String = ""
): String {
    return """
        function main() {
          $funcBody
          return
        }
        $body
    """.trimIndent()
}

private fun normStatements(
    funcBody: List<String> = emptyList(),
    body: List<String> = emptyList()
): List<String> {
    return listOf(
        "function main ( ) {",
        *funcBody.toTypedArray(),
        "return",
        "}",
        *body.toTypedArray()
    )
}

private fun normStatements(
    funcBody: String? = null,
    body: String? = null
) = normStatements(listOfNotNull(funcBody), listOfNotNull(body))

private fun String.normalized(): List<String> {
    return processor.split(this)
        .mapNotNull { processor.proceess(it) }
        .onEach { println(it.accept(AstPrintVisitor)) }
        .onEach { println(it.accept(AstXmlBuildVisitor)) }
        .map { it.getText() }
}