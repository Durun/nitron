package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import io.github.durun.nitron.core.ast.basic.AstRuleNode
import java.io.PrintStream
import java.nio.file.Path

class CodeNormalizeCommand : CliktCommand(
        name = "normalize"
) {
    private val inputPath: Path by argument(
            name = "input",
            help = "input file to parse"
    ).path()

    private val configPath: Path by option(
            names = *arrayOf("--config", "-c"),
            help = "config file (.json)"
    ).path().required()

    private val outputPath: Path? by option(
            names = *arrayOf("--output", "-o"),
            help = "output file"
    ).path()

    private val output: PrintStream = PrintStream(
            outputPath?.toFile()?.outputStream()
                    ?: System.out
    )

    override fun run() {
        val processor = CodeProcessor(configPath)
        val input = inputPath.toFile().inputStream().bufferedReader().readText()
        val texts = processor.process(input).map { (node, text) ->
            val ruleName = if (node is AstRuleNode) node.ruleName else null
            "[${ruleName}]\n${text.prependIndent("\t")}"
        }
        output.println(texts.joinToString("\n"))
    }
}