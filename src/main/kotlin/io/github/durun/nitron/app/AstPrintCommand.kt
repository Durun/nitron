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
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.CommonParser
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class AstPrintCommand : CliktCommand(
        name = "print"
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
        val parser = CommonParser(
                grammarFiles = config.grammar.grammarFilePaths,
                utilityJavaFiles = config.grammar.utilJavaFilePaths
        )
        inputs
                .forEach { input ->
                    val tree = parser.parse(input.readText(), config.grammar.startRule)
                    val ast = tree.accept(AstBuildVisitor(grammarName = config.fileName, parser = parser.getAntlrParser()))
                    val text = ast.accept(AstPrintVisitor)
                    output.println("\n@ ${input.path}")
                    output.println(text)
                }
    }
}