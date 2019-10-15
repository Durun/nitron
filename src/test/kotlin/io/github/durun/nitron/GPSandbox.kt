package io.github.durun.nitron

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.durun.nitron.ast.basic.AstBuildVisitor
import io.github.durun.nitron.ast.basic.BasicAstRuleNode
import io.github.durun.nitron.ast.normalizing.normalizeByRules
import org.snt.inmemantlr.GenericParser
import org.snt.inmemantlr.listener.DefaultListener
import java.nio.file.Paths

fun main() {
    val dir = Paths.get("testdata/grammars/kotlin")
    val gParser = GenericParser(
            dir.resolve("KotlinLexer.g4").toFile(),
            dir.resolve("KotlinParser.g4").toFile(),
            dir.resolve("UnicodeClasses.g4").toFile()

    )
    val input = """
        fun main() {
            val str = "bye"
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
    val production = "kotlinFile"


    gParser.compile()
    println("compiled.")
    val listener = ParserListener()
    gParser.setListener(listener)

    val result = gParser.parse(input, production, GenericParser.CaseSensitiveType.NONE)
    val parser = listener.getParser() ?: throw Exception("can't get parser before parse")
    val ast = result.accept(AstBuildVisitor(parser))

    val mapper = jacksonObjectMapper()
    val json = mapper.writeValueAsString(ast)
    println(json)

    val pickRules = listOf("statement")
    println("pick by: $pickRules")
    val normAst = ast.pickRecursiveByRules(pickRules).map { it.normalizeByRules(listOf("stringLiteral", "variableDeclaration")) }
    println(
            normAst.filterIsInstance<BasicAstRuleNode>()
                    .joinToString("\n") {
                        "[${it.ruleName}]\n${it.getText()?.prependIndent("\t")}"
                    }
    )
    println(mapper.writeValueAsString(normAst))
}


class ParserListener(): DefaultListener(){
    fun getParser() = this.parser ?: null
}