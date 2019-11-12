package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class CodeNormalizeCommand : CliktCommand(
        name = "normalize"
) {
    private val inputs: List<File> by argument(
            name = "input",
            help = "input file to parse"
    ).file(
            readable = true
    ).multiple()

    private val configPath: Path by option(
            names = *arrayOf("--config", "-c"),
            help = "config file (.json)"
    ).path(
            readable = true
    ).required()

    private val outputPath: Path? by option(
            names = *arrayOf("--output", "-o"),
            help = "output file"
    ).path(
            writable = true
    )

    private val output: PrintStream = PrintStream(
            outputPath?.toFile()?.outputStream()
                    ?: System.out
    )

    override fun run() {
        val config = LangConfigLoader.load(configPath)
        val processor = CodeProcessor(config)
        inputs
                .forEach { input ->
                    val text = processor.processText(input.readText())
                    output.println("\n@ ${input.path}")
                    output.println(text)
                }
    }

    private fun CodeProcessor.processText(input: String): String {
        val ast = this.parse(input)
        val astList = this.split(ast)
        val texts = this.proceess(astList).map {
            val ruleName = if (it is AstRuleNode) it.ruleName else null
            val text = it.getText().orEmpty()
            "[${ruleName}]\n${text.prependIndent("\t")}"
        }
        return texts.joinToString("\n")
    }
}