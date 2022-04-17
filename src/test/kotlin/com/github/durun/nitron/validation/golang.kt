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
}


private fun code(
    funcBody: String = ""
): String {
    return """package main
        import "fmt"
        func main() {
          $funcBody
        }
    """.trimIndent()
}

private fun normStatements(
    funcBody: List<String> = emptyList()
): List<String> {
    return listOf(
        "func main ( ) {",
        *funcBody.toTypedArray()
    )
}

private fun normStatements(
    funcBody: String? = null
) = normStatements(listOfNotNull(funcBody))

private fun String.normalized(): List<String> {
    return processor.split(this)
        .mapNotNull { processor.proceess(it) }
        .onEach { println(it.accept(AstPrintVisitor)) }
        .onEach { println(it.accept(AstXmlBuildVisitor)) }
        .map { it.getText() }
}