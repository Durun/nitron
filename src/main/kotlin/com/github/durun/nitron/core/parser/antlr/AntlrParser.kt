@file:Suppress("unused")

package com.github.durun.nitron.core.parser.antlr

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.parser.NitronParser
import com.github.durun.nitron.core.parser.NitronParsers
import org.antlr.v4.runtime.tree.ParseTreeVisitor
import java.io.Reader
import java.nio.file.Path

private class AntlrParser
private constructor(
    override val nodeTypes: NodeTypePool,
    private val genericParser: GenericParser,
    private val buildVisitor: ParseTreeVisitor<AstNode>,
    private val defaultEntryPoint: String
) : NitronParser {
    companion object {
        fun init(
            grammarName: String,
            entryPoint: String,
            grammarFiles: Collection<Path>,
            utilityJavaFiles: Collection<Path> = emptySet(),
        ): NitronParser {
            val genericParser = GenericParser.fromFiles(grammarFiles, utilityJavaFiles)
            val buildVisitor = AstBuildVisitor(grammarName, genericParser.antlrParser)
            return AntlrParser(buildVisitor.nodeTypes, genericParser, buildVisitor, entryPoint)
        }
    }

    override fun parse(reader: Reader): AstNode {
        val tree = genericParser.parse(reader, defaultEntryPoint)
        return tree.accept(buildVisitor)
    }
}

fun antlr(
    grammarName: String,
    entryPoint: String,
    grammarFiles: Collection<Path>,
    utilityJavaFiles: Collection<Path> = emptySet()
): NitronParser = NitronParsers.antlr(grammarName, entryPoint, grammarFiles, utilityJavaFiles)

fun NitronParsers.antlr(
    grammarName: String,
    entryPoint: String,
    grammarFiles: Collection<Path>,
    utilityJavaFiles: Collection<Path> = emptySet()
): NitronParser = AntlrParser.init(grammarName, entryPoint, grammarFiles, utilityJavaFiles)