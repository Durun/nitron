package io.github.durun.nitron.binding.cpanalyzer

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.visitor.AstIgnoreVisitor
import io.github.durun.nitron.core.ast.visitor.AstSplitVisitor
import io.github.durun.nitron.core.ast.visitor.AstVisitor
import io.github.durun.nitron.core.ast.visitor.normalizing.AstNormalizeVisitor
import io.github.durun.nitron.core.ast.visitor.normalizing.NormalizingRuleMap
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.CommonParser
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.ast.table.NodeTypeSets
import io.github.durun.nitron.inout.model.ast.table.Structures
import io.github.durun.nitron.inout.model.ast.table.StructuresJsonWriter
import io.github.durun.nitron.inout.model.ast.table.StructuresWriter
import io.github.durun.nitron.inout.model.ast.toSerializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path

class CodeProcessor(
        config: LangConfig,
        outputPath: Path? = null    // TODO recording feature should be separated
) {

    private val parser: CommonParser
    private val startRule: String
    private val splitVisitor: AstVisitor<List<AstNode>>
    private val nonNumberedRuleMap: NormalizingRuleMap
    private val numberedRuleMap: NormalizingRuleMap
    private val ignoreVisitor: AstVisitor<AstNode?>
    private val recorder: JsonCodeRecorder? // TODO recording feature should be separated

    init {
        parser = CommonParser(
                grammarFiles = config.grammar.grammarFilePaths,
                utilityJavaFiles = config.grammar.utilJavaFilePaths
        )
        startRule = config.grammar.startRule
        splitVisitor = AstSplitVisitor(config.process.splitConfig.splitRules)
        nonNumberedRuleMap = config.process.normalizeConfig.nonNumberedRuleMap
        numberedRuleMap = config.process.normalizeConfig.numberedRuleMap
        ignoreVisitor = AstIgnoreVisitor(config.process.normalizeConfig.ignoreRules)
        println("Parser compiled: config=${config.dir}")   // TODO

        recorder = outputPath?.let {
            JsonCodeRecorder(
                    nodeTypeSet = NodeTypeSet(
                            grammarName = config.fileName,
                            parser = parser.getAntlrParser()
                    ),
                    destination = it
            )
        }
    }

    fun parse(input: String): AstNode {
        val (tree, antlrParser) = parser.parse(input, startRule)
        return tree.accept(AstBuildVisitor(antlrParser))
    }

    fun split(input: AstNode): List<AstNode> {
        return input.accept(splitVisitor)
    }

    fun split(input: String): List<AstNode> {
        val ast = parse(input)
        return split(ast)
    }

    private fun dropIgnore(input: AstNode): AstNode? {
        return input
                .accept(ignoreVisitor)
    }

    private fun normalize(input: AstNode): AstNode {
        val visitor = AstNormalizeVisitor(nonNumberedRuleMap, numberedRuleMap)
        return input.accept(visitor)
    }

    fun proceess(input: AstNode): AstNode? {
        return dropIgnore(input)
                ?.let { normalize(it) }
    }

    fun proceess(input: List<AstNode>): List<AstNode> {
        return input.mapNotNull { proceess(it) }
    }

    fun proceessWithOriginal(input: List<AstNode>): List<Pair<AstNode, AstNode?>> {
        return input.map {
            it to proceess(it)
        }
    }

    fun write(ast: AstNode) {   // TODO recording feature should be separated
        (recorder ?: throw IllegalStateException("CodeRecorder is not initialized."))
                .write(ast)
    }

    fun write(asts: Iterable<AstNode>) {   // TODO recording feature should be separated
        (recorder ?: throw IllegalStateException("CodeRecorder is not initialized."))
                .write(asts)
    }
}

private class JsonCodeRecorder(
        private val nodeTypeSet: NodeTypeSet,
        destination: Path
) {
    private val writer: StructuresJsonWriter

    init {
        val file = destination.toFile()
        writer = StructuresJsonWriter(file, nodeTypeSet)
    }

    fun write(ast: AstNode) {
        val structure = ast.toSerializable(nodeTypeSet)
        writer.write(structure)
    }

    fun write(asts: Iterable<AstNode>) {
        val structures = asts.map {
            it.toSerializable(nodeTypeSet)
        }
        writer.write(structures)
    }
}

@Deprecated("CodeRecorder runs very slowly.")
private class CodeRecorder(
        private val nodeTypeSet: NodeTypeSet,
        destination: Database
) {
    private val writer = StructuresWriter(destination)

    private fun initTables(db: Database) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(NodeTypeSets, Structures)
        }
    }

    init {
        initTables(destination)
    }

    fun write(ast: AstNode) {
        val structure = ast.toSerializable(nodeTypeSet)
        writer.write(structure)
    }
}