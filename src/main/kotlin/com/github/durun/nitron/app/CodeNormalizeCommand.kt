package com.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.config.loader.LangConfigLoader
import java.io.BufferedWriter
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedWriter

class CodeNormalizeCommand : CliktCommand(
    name = "normalize"
) {
    private val inputs: List<File> by argument(
        name = "input",
        help = "input file to parse"
    ).file(
        mustBeReadable = true
    ).multiple()

    private val configPath: Path by option(
        "--config", "-c",
        help = "config file (.json)"
    ).path(
        mustBeReadable = true
    ).required()

    private val outputPath: Path? by option(
        "--output", "-o",
        help = "output file"
    ).path(
        mustBeWritable = true
    )

    override fun run() {
        val output: BufferedWriter = outputPath?.bufferedWriter()
            ?: System.out.bufferedWriter()
        val config = LangConfigLoader.load(configPath)
        val processor = CodeProcessor(config)
        inputs
            .forEach { input ->
                val text = processor.processText(input.readText())
                output.appendLine("\n@ ${input.path}")
                output.appendLine(text)
            }
        output.flush()
    }

    private fun CodeProcessor.processText(input: String): String {
        val ast = this.parse(input)
        val astList = this.split(ast)
        val texts = this.proceess(astList).map {
            val ruleName = if (it is AstRuleNode) it.type.name else null
            val text = it.getText()
            "[${ruleName}]\n${text.prependIndent("\t")}"
        }
        return texts.joinToString("\n")
    }
}