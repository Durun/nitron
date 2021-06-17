package io.github.durun.nitron.core.parser.antlr

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.parser.AstBuilder
import org.antlr.v4.runtime.tree.ParseTreeVisitor
import java.io.BufferedReader
import java.nio.file.Path

class AntlrAstBuilder
private constructor(
    private val genericParser: GenericParser,
    private val buildVisitor: ParseTreeVisitor<AstNode>,
    private val defaultEntryPoint: String
) : AstBuilder {
    companion object {
        fun init(
            grammarName: String,
            entryPoint: String,
            grammarFiles: Collection<Path>,
            utilityJavaFiles: Collection<Path> = emptySet(),
        ): AstBuilder {
            val genericParser = GenericParser.fromFiles(grammarFiles, utilityJavaFiles)
            return AntlrAstBuilder(
                genericParser,
                AstBuildVisitor(grammarName, genericParser.antlrParser),
                entryPoint
            )
        }
    }

    override fun parse(reader: BufferedReader, entryPoint: String?): AstNode {
        val tree = genericParser.parse(reader, entryPoint ?: defaultEntryPoint)
        return tree.accept(buildVisitor)
    }
}