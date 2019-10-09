package io.github.durun.nitron.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.ast.basic.AstBuildVisitor
import io.github.durun.nitron.getGrammarList
import io.github.durun.nitron.parser.CommonParser
import java.io.PrintStream
import java.nio.file.Path

class ParseToJsonApp: CliktCommand() {
    val grammarDir: Path by argument().path()
    val inputPath: Path by argument().path()
    val outputPath: Path? by option().path()
    val startRule: String? by option()

    val output = PrintStream(
            outputPath?.toFile()?.outputStream()
                    ?: System.out
    )

    override fun run() {
        val parser = CommonParser(getGrammarList(grammarDir))
        val (tree, antlrParser) = parser.parse(inputPath, startRule)
        val ast = tree.accept(AstBuildVisitor(antlrParser))
        val json = jacksonObjectMapper().writeValueAsString(ast)
        output.println(json)
    }
}
fun main(args: Array<String>) = ParseToJsonApp().main(args)