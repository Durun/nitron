package io.github.durun.nitron.core.parser.antlr

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.parser.AstBuilder
import io.github.durun.nitron.core.parser.AstBuilders
import org.antlr.v4.runtime.tree.ParseTreeVisitor
import java.io.Reader
import java.nio.file.Path

class AntlrAstBuilder
private constructor(
    override val nodeTypes: NodeTypePool,
    private val genericParser: GenericParser,
    private val buildVisitor: ParseTreeVisitor<AstNode>,
    private val defaultEntryPoint: String
) : AstBuilder {
    companion object {
        internal fun init(
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

    override fun parse(reader: Reader, entryPoint: String?): AstNode {
        val tree = genericParser.parse(reader, entryPoint ?: defaultEntryPoint)
        return tree.accept(buildVisitor)
    }
}

fun AstBuilders.antlr(
    grammarName: String,
    entryPoint: String,
    grammarFiles: Collection<Path>,
    utilityJavaFiles: Collection<Path> = emptySet()
): AstBuilder = AntlrAstBuilder.init(grammarName, entryPoint, grammarFiles, utilityJavaFiles)