package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.github.durun.nitron.core.parser.AstBuilders
import io.github.durun.nitron.core.parser.antlr.antlr
import java.io.BufferedWriter
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedWriter

class AstPrintCommand : CliktCommand(
        name = "print"
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

    @ExperimentalPathApi
    private val output: BufferedWriter = outputPath?.bufferedWriter()
            ?: System.out.bufferedWriter()

    @ExperimentalPathApi
    override fun run() {
        val config = LangConfigLoader.load(configPath)
        val astBuilder = AstBuilders.antlr(
            grammarName = config.fileName,
            entryPoint = config.grammar.startRule,
            grammarFiles = config.grammar.grammarFilePaths,
            utilityJavaFiles = config.grammar.utilJavaFilePaths
        )
        inputs
            .forEach { input ->
                val ast = astBuilder.parse(input.bufferedReader())
                val text = ast.accept(AstPrintVisitor)
                output.appendLine("\n@ ${input.path}")
                output.appendLine(text)
            }
        output.flush()
    }
}