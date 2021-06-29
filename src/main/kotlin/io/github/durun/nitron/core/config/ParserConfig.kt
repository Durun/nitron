package io.github.durun.nitron.core.config

import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.parser.AstBuilder
import io.github.durun.nitron.core.parser.AstBuilders
import io.github.durun.nitron.core.parser.antlr.antlr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path


@Serializable
sealed class GrammarConfig : ConfigWithDir() {
    abstract fun getParser(): AstBuilder
    abstract fun checksum(): MD5
}

@Serializable
@SerialName("antlr")
data class AntlrParserConfig(
    private val grammarFiles: List<String>,
    private val utilJavaFiles: List<String>,
    val startRule: String
) : GrammarConfig() {
    val grammarFilePaths: List<Path> by lazy {
        grammarFiles.map { dir.resolve(it) }
    }
    val utilJavaFilePaths: List<Path> by lazy {
        utilJavaFiles.map { dir.resolve(it) }
    }

    override fun getParser(): AstBuilder = AstBuilders.antlr(
        grammarName = fileName,
        entryPoint = startRule,
        grammarFiles = grammarFilePaths,
        utilityJavaFiles = utilJavaFilePaths
    )

    override fun checksum(): MD5 {
        val paths = grammarFilePaths + utilJavaFilePaths
        return paths.map { MD5.digest(it.toFile().readText()).toString() }
            .sorted()
            .reduce { a, b -> a + b }
            .let { MD5.digest(it) }
    }
}