@file:Suppress("unused")

package com.github.durun.nitron.core.parser.antlr

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.parser.AstBuilder
import com.github.durun.nitron.core.parser.AstBuilders
import org.antlr.v4.runtime.tree.ParseTreeVisitor
import java.io.Reader
import java.nio.file.Path

private class AntlrAstBuilder
private constructor(
    override val nodeTypes: NodeTypePool,
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
            val buildVisitor = AstBuildVisitor(grammarName, genericParser.antlrParser)
            return AntlrAstBuilder(buildVisitor.nodeTypes, genericParser, buildVisitor, entryPoint)
        }
    }

    override fun parse(reader: Reader): AstNode {
        val tree = genericParser.parse(reader, defaultEntryPoint)
        return tree.accept(buildVisitor)
    }
}

fun AstBuilders.antlr(
    grammarName: String,
    entryPoint: String,
    grammarFiles: Collection<Path>,
    utilityJavaFiles: Collection<Path> = emptySet()
): AstBuilder = AntlrAstBuilder.init(grammarName, entryPoint, grammarFiles, utilityJavaFiles)