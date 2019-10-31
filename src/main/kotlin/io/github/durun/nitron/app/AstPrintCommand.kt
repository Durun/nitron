package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.ast.basic.AstBuildVisitor
import io.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import io.github.durun.nitron.core.config.LangConfigLoader
import io.github.durun.nitron.core.parser.CommonParser
import java.io.PrintStream
import java.nio.file.Path

class AstPrintCommand : CliktCommand(
        name = "print"
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
        val config = LangConfigLoader.load(configPath)
        val baseDir = configPath.toAbsolutePath().parent
        val parser = CommonParser(
                grammarFiles = config.grammarConfig.grammarFilePaths(baseDir),
                utilityJavaFiles = config.grammarConfig.utilJavaFilesPaths(baseDir)
        )
        val (tree, antlrParser) = parser.parse(inputPath, config.grammarConfig.startRule)
        val ast = tree.accept(AstBuildVisitor(antlrParser))
        output.println(ast.accept(AstPrintVisitor()))
    }
}