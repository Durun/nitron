@file:Suppress("unused")

package com.github.durun.nitron.core.parser.antlr

import com.github.durun.nitron.core.ParsingException
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
        fun fromContents(
            grammarName: String,
            entryPoint: String,
            grammarFileContents: Collection<String>,
            utilityJavaContents: Collection<String> = emptySet(),
        ): NitronParser {
            val genericParser = GenericParser.init(grammarFileContents, utilityJavaContents)
            val buildVisitor = AstBuildVisitor(grammarName, genericParser.antlrParser)
            return AntlrParser(buildVisitor.nodeTypes, genericParser, buildVisitor, entryPoint)
        }

        fun fromFiles(
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
        val tree = try {
            genericParser.parse(reader, defaultEntryPoint)
        } catch (e: org.snt.inmemantlr.exceptions.ParsingException) {
            throw ParsingException("Failed to parse with ANTLR: ${e.message}", e)
        } catch (e: Exception) {
            throw ParsingException("Internal error: ${e.message}", e)
        }
        val ast = try {
            tree.accept(buildVisitor)
        } catch (e: Exception) {
            throw ParsingException("Failed to convert ANTLR tree into nitron tree")
        }
        return ast
    }
}

fun fromContents(
    grammarName: String,
    entryPoint: String,
    grammarFileContents: Collection<String>,
    utilityJavaContents: Collection<String> = emptySet()
): NitronParser = AntlrParser.fromContents(grammarName, entryPoint, grammarFileContents, utilityJavaContents)

fun fromPaths(
    grammarName: String,
    entryPoint: String,
    grammarFilePaths: Collection<Path>,
    utilityJavaPaths: Collection<Path> = emptySet()
): NitronParser = AntlrParser.fromFiles(grammarName, entryPoint, grammarFilePaths, utilityJavaPaths)

fun NitronParsers.antlr(
    grammarName: String,
    entryPoint: String,
    grammarFileContents: Collection<String>,
    utilityJavaContents: Collection<String> = emptySet()
): NitronParser = AntlrParser.fromContents(grammarName, entryPoint, grammarFileContents, utilityJavaContents)