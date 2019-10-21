package io.github.durun.nitron.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.ast.basic.AstBuildVisitor
import io.github.durun.nitron.getGrammarList
import io.github.durun.nitron.parser.CommonParser
import java.io.PrintStream
import java.nio.file.Path

class App : CliktCommand() {
    private val grammarDir: Path by argument(
            name = "grammar_dir",
            help = "directory containing grammar(.g4) files"
    ).path()
    private val inputPath: Path by argument(
            name = "input",
            help = "input file to parse"
    ).path()
    private val outputPath: Path? by option(
            names = *arrayOf("--output", "-o"),
            help = "output file"
    ).path()
    private val startRule: String by option(
            names = *arrayOf("--start", "-s"),
            help = "the start rule(defined in grammar file)"
    ).required()

    private val output = PrintStream(
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

fun main(args: Array<String>) = App().main(args)