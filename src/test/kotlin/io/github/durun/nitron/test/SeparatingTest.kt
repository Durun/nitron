package io.github.durun.nitron.test

import io.github.durun.nitron.core.ast.basic.AstBuildVisitor
import io.github.durun.nitron.core.ast.basic.BasicAstRuleNode
import io.github.durun.nitron.core.getGrammarList
import io.github.durun.nitron.core.parser.CommonParser
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.nio.file.Paths

class SeparatingTest : StringSpec({
    "kotlin separating statements" {
        val grammarDir = Paths.get("testdata/grammars/kotlin")
        val parser = CommonParser(getGrammarList(grammarDir))
        val startRule = "kotlinFile"
        val input = """
        fun main() {
            val str = "This is string."                                 // 1
            println("Hello.")                                           // 2
            if (true) {                                                 // 3
                println(str)                                            // 4
                if (false) println("---") else println("if expression") // 5
                if (true) {                                             // 6
                    println("if statement") }                           // 7
                else {
                    println("---")                                      // 8
                }
            }
        }
        """.trimIndent()
        val (tree, antlrParser) = parser.parse(input, startRule)
        val ast = tree.accept(AstBuildVisitor(antlrParser))
        // separate by statement
        val asts = ast.pickRecursiveByRules(rules = listOf("statement"))
        asts.size shouldBe 8
        val statements = asts.filterIsInstance<BasicAstRuleNode>()
        statements.size shouldBe 8
        println(statements.joinToString("\n") { "[${it.ruleName}]\n${it.getText()?.prependIndent("\t")}" })
    }
})