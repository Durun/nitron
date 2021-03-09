package io.github.durun.nitron.binding.cpanalyzer

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.visitor.AstVisitor
import io.github.durun.nitron.core.ast.visitor.astIgnoreVisitorOf
import io.github.durun.nitron.core.ast.visitor.astSplitVisitorOf
import io.github.durun.nitron.core.ast.visitor.normalizing.AstNormalizeVisitor
import io.github.durun.nitron.core.ast.visitor.normalizing.astNormalizeVisitorOf
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.GenericParser
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.ast.merge
import io.github.durun.nitron.inout.model.ast.table.StructuresJsonWriter
import java.nio.file.Path

class CodeProcessor(
		config: LangConfig,
		outputPath: Path? = null    // TODO recording feature should be separated
) {
	val nodeTypePool: NodeTypePool
	private val parser: GenericParser
	private val nodeBuilder: AstBuildVisitor
	private val startRule: String
	private val splitVisitor: AstVisitor<List<AstNode>>
	private val ignoreVisitor: AstVisitor<AstNode?>
	private val normalizer: AstNormalizeVisitor
	private val recorder: JsonCodeRecorder? // TODO recording feature should be separated

	init {
		parser = GenericParser.fromFiles(
				grammarFiles = config.grammar.grammarFilePaths,
				utilityJavaFiles = config.grammar.utilJavaFilePaths
		)
		nodeBuilder = AstBuildVisitor(grammarName = config.fileName, parser = parser.antlrParser)
		nodeTypePool = nodeBuilder.nodeTypes
		startRule = config.grammar.startRule
		splitVisitor = astSplitVisitorOf(types = nodeTypePool, splitTypes = config.process.splitConfig.splitRules)
		ignoreVisitor = astIgnoreVisitorOf(types = nodeTypePool, ignoreTypes = config.process.normalizeConfig.ignoreRules)
		normalizer = astNormalizeVisitorOf(
				nonNumberedRuleMap = config.process.normalizeConfig.nonNumberedRuleMap,
				numberedRuleMap = config.process.normalizeConfig.numberedRuleMap,
				types = nodeTypePool)

		recorder = outputPath?.let {
			JsonCodeRecorder(
					nodeTypePool = nodeTypePool,
					destination = it
			)
		}
	}

	fun parse(input: String): AstNode {
		val tree = parser.parse(input.reader(), startRule)
		return tree.accept(nodeBuilder)
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
		return normalizer.normalize(input)
	}

	fun proceess(input: AstNode): AstNode? {
		return dropIgnore(input)
				?.let { normalize(it) }
	}

	fun proceess(input: List<AstNode>): List<AstNode> {
		return input.mapNotNull { proceess(it) }
	}

	@Deprecated("This method can return incorrect result.")
	fun proceessWithOriginal(input: List<AstNode>): List<Pair<AstNode, AstNode?>> {
		return input.map {
			it to proceess(it)
		}
	}

	fun write(asts: Iterable<AstNode>) {   // TODO recording feature should be separated
		(recorder ?: throw IllegalStateException("CodeRecorder is not initialized."))
				.write(asts)
	}
}

class JsonCodeRecorder(
        private val nodeTypePool: NodeTypePool,
        destination: Path
) {
    private val writer: StructuresJsonWriter

    init {
        val file = destination.toFile()
        writer = StructuresJsonWriter(file, nodeTypePool)
    }

    fun write(ast: AstNode) {
        val structure = Structure(nodeTypePool, ast)
        writer.write(structure)
    }

    fun write(asts: Iterable<AstNode>) {
        val structures = asts.map {
            Structure(nodeTypePool, it)
        }
        merge(structures)
                ?.let { writer.write(it) }
    }
}
