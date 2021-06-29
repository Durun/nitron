package io.github.durun.nitron.binding.cpanalyzer

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.path.AstPath
import io.github.durun.nitron.core.ast.processors.AstNormalizer
import io.github.durun.nitron.core.ast.processors.AstSplitter
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.parser.AstBuilder
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.ast.merge
import io.github.durun.nitron.inout.model.ast.table.StructuresJsonWriter
import java.nio.file.Path

class CodeProcessor(
    config: LangConfig,
    outputPath: Path? = null    // TODO recording feature should be separated
) {
    private val astBuilder: AstBuilder = config.parserConfig.getParser()
    val nodeTypePool: NodeTypePool = astBuilder.nodeTypes
    private val splitter = ThreadLocal.withInitial {
        AstSplitter(config.process.splitConfig.splitRules.mapNotNull { nodeTypePool.getType(it) })
    }
    private val normalizer = ThreadLocal.withInitial {
        AstNormalizer(
            mapping = config.process.normalizeConfig.mapping.entries.associate { (path, symbol) ->
                AstPath.of(path, nodeTypePool) to symbol
            },
            numberedMapping = config.process.normalizeConfig.indexedMapping.entries.associate { (path, symbol) ->
                AstPath.of(path, nodeTypePool) to symbol
            },
            ignoreRules = config.process.normalizeConfig.ignoreRules.map {
                AstPath.of(it, nodeTypePool)
            }
        )
    }

    // TODO recording feature should be separated
    private val recorder: JsonCodeRecorder? = outputPath?.let {
        JsonCodeRecorder(
            nodeTypePool = nodeTypePool,
            destination = it
        )
    }

    fun parse(input: String): AstNode {
        return astBuilder.parse(input.reader().buffered())
    }

    fun split(input: AstNode): List<AstNode> {
        return splitter.get().process(input)
    }

    fun split(input: String): List<AstNode> {
        val ast = parse(input)
        return split(ast)
    }

    private fun normalize(input: AstNode): AstNode? {
        return normalizer.get().process(input)
    }

    fun proceess(input: AstNode): AstNode? {
        return normalize(input)
    }

    fun proceess(input: List<AstNode>): List<AstNode> {
        return input.mapNotNull { proceess(it) }
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
