package io.github.durun.nitron

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.durun.nitron.ast.basic.AstBuildVisitor
import io.github.durun.nitron.ast.normalizing.NormalizePrintVisitor
import io.github.durun.nitron.parser.CommonParser
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
            nonNumberedRuleMap = mapOf("stringLiteral" to "S"),
            numberedRuleMap = mapOf("variableDeclaration" to "\$V", "primaryExpression" to "\$V")
    ))
    println(ast.getText())
    println(normStr)
}
