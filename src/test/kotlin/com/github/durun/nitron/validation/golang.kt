package com.github.durun.nitron.validation

import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.matchers.shouldBe
import java.nio.file.Paths


private val configPath = Paths.get("config/nitron.json")
private val config = NitronConfigLoader.load(configPath).langConfig["golang"] ?: throw Exception()

private val processor = CodeProcessor(config)

fun main() = testReportAsMarkDown {
    "package" - {
        "ignore package" {
            "package sample".normalized() shouldBe emptyList()
        }
        "ignore import" {
            """
            |package main
            |import "fmt"
            |""".trimMargin().normalized() shouldBe emptyList()
        }
    }
    "literal" - {
        "string" {
            code(funcBody = """"hello"""").normalized() shouldBe normStatements(funcBody = """"S"""")
        }
        "rune" {
            code(funcBody = "'A'").normalized() shouldBe normStatements(funcBody = "'R'")
        }
        "integer" {
            code(funcBody = "123").normalized() shouldBe normStatements(funcBody = "N")
            code(funcBody = "0b1").normalized() shouldBe normStatements(funcBody = "N")
            code(funcBody = "0o1").normalized() shouldBe normStatements(funcBody = "N")
            code(funcBody = "0x1").normalized() shouldBe normStatements(funcBody = "N")
        }
        "floating point" {
            code(funcBody = "1.2").normalized() shouldBe normStatements(funcBody = "F")
        }
        "imaginary" {
            code(funcBody = "12i").normalized() shouldBe normStatements(funcBody = "I")
        }
    }
    "variable" - {
        "assignment" {
            code(funcBody = "x = f()").normalized() shouldBe normStatements(funcBody = "V0 = f ( )")
            code(funcBody = "x, y = f()").normalized() shouldBe normStatements(funcBody = "V0 , V1 = f ( )")
        }
        "declaration" {
            code(funcBody = "var i int = 1").normalized() shouldBe normStatements(funcBody = "var V0 int = N")
            code(funcBody = "var i, j int = 1, 2").normalized() shouldBe normStatements(funcBody = "var V0 , V1 int = N , N")
        }
        "short declaration" {
            code(funcBody = "x := 1").normalized() shouldBe normStatements(funcBody = "V0 := N")
            code(funcBody = "i, j := 1, 2").normalized() shouldBe normStatements(funcBody = "V0 , V1 := N , N")
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
        "struct access" {
            code(funcBody = "v.m = f()").normalized() shouldBe normStatements(funcBody = "V0 . m = f ( )")
            code(funcBody = "f(v.name.length)").normalized() shouldBe normStatements(funcBody = "f ( V0 . name . length )")
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
        "while block" {
            code(
                funcBody = """
                for i < 5 {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("for V0 < N {", "call ( )", "}"))
        }
        "forever block" {
            code(
                funcBody = """
                for {
                  break
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("for {", "break", "}"))
        }
        "for block" {
            code(
                funcBody = """
                for i := 0; i < 10; i++ {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("for V0 := N ; V0 < N ; V0 ++ {", "call ( )", "}"))
        }
        "for range block" {
            code(
                funcBody = """
                for i, v := range l {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("for V0 , V1 := range V2 {", "call ( )", "}"))
        }
        "if block" {
            code(
                funcBody = """
                if cond {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("if V0 {", "call ( )", "}"))
        }
        "if with short statement" {
            code(
                funcBody = """
                if v := 1; cond {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(funcBody = listOf("if V0 := N ; V1 {", "call ( )", "}"))
        }
        "if-else block" {
            code(
                funcBody = """
                if cond {
                  call()
                } else {
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "if V0 {",
                    "call ( )",
                    "} else {",
                    "call ( )",
                    "}"
                )
            )
        }
        "switch" {
            code(
                funcBody = """
                switch x := 1; x {
                case 1:
                  call()
                default:
                  call()
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "switch V0 := N ; V0 {",
                    "case N :",
                    "call ( )",
                    "default :",
                    "call ( )",
                    "}"
                )
            )
        }
        "return statement" {
            code(funcBody = "return call()").normalized() shouldBe normStatements(funcBody = "return call ( )")
        }
        "defer statement" {
            code(funcBody = "defer call()").normalized() shouldBe normStatements(funcBody = "defer call ( )")
        }
    }
    "data" - {
        "pointer" {
            code(funcBody = "*p = &i").normalized() shouldBe normStatements(funcBody = "* V0 = & V1")
        }
        "struct" {
            code(funcBody = "Vec{1, 2}").normalized() shouldBe normStatements(funcBody = "Vec { N , N }")
            code(funcBody = "Vec{X: 1}").normalized() shouldBe normStatements(funcBody = "Vec { X : N }")
        }
        "array" {
            code(funcBody = "a[0]").normalized() shouldBe normStatements(funcBody = "V0 [ N ]")
            code(funcBody = "a[1:2]").normalized() shouldBe normStatements(funcBody = "V0 [ N : N ]")
        }
        "map" {
            code(funcBody = "map[int]int{ 1: 1 }").normalized() shouldBe normStatements(funcBody = "map [ int ] int { N : N }")
        }
        "closure" {
            code(funcBody = "func(x int) int { return x }").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "func ( V0 int ) int {",
                    "return V0",
                    "}"
                )
            )
        }
    }
    "declaration" - {
        "function" {
            code(
                body = """
                func f(i int, fn func(int) int) int {
                    return 0
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                body = listOf(
                    "func f ( V0 int , V1 func ( int ) int ) int {",
                    "return N",
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
    return """package main
        import "fmt"
        func main() {
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
        "func main ( ) {",
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