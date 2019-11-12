package io.github.durun.nitron

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.durun.nitron.core.ast.AstBuildVisitor
import io.github.durun.nitron.core.ast.visitor.NormalizePrintVisitor
import io.github.durun.nitron.core.ast.normalizing.NormalizingRuleMap
import io.github.durun.nitron.core.getGrammarList
import io.github.durun.nitron.core.parser.CommonParser
import java.nio.file.Paths

fun main() {
    val dir = Paths.get("testdata/grammars/kotlin-formal")
    val parser = CommonParser(getGrammarList(dir))
    val startRule = "kotlinFile"
    val input = """
        fun main() {
            val str = "bye"
            var n = str.size
            var n2 = str.toUpperCase().size
            n = n + n2 + 1
            println("hello")
            if (true) {
                print(str)
                when(str) {
                    "hello" -> print("hoge")
                    "bye" -> print("yahoo")
                    else -> println("aho")
                }
                if(false) println("FALSE")
                else println("???")
                if(false) { println("FALSE") }
                else { println("???") }
            }
        }
        """.trimIndent()//dir.resolve("examples/Test.kt").toFile()
    val (tree, antlrParser) = parser.parse(input, startRule)
    val ast = tree.accept(AstBuildVisitor(antlrParser))

    val mapper = jacksonObjectMapper()
    val json = mapper.writeValueAsString(ast)
    println(json)

    val normStr = ast.accept(NormalizePrintVisitor(
            nonNumberedRuleMap = NormalizingRuleMap(
                    listOf("stringLiteral") to "S"
            ),
            numberedRuleMap = NormalizingRuleMap(
                    listOf("variableDeclaration") to "\$V",
                    listOf("primaryExpression") to "\$V"
            )
    ))
    println(ast.getText())
    println(normStr)
}
