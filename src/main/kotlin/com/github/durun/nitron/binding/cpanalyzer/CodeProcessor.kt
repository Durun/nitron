package com.github.durun.nitron.binding.cpanalyzer

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.path.AstPath
import com.github.durun.nitron.core.ast.processors.AstNormalizer
import com.github.durun.nitron.core.ast.processors.AstSplitter
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.config.LangConfig
import com.github.durun.nitron.core.parser.NitronParser
import com.github.durun.nitron.inout.model.ast.Structure
import com.github.durun.nitron.inout.model.ast.merge
import com.github.durun.nitron.inout.model.ast.table.StructuresJsonWriter
import java.nio.file.Path

class CodeProcessor(
    config: LangConfig,
    outputPath: Path? = null    // TODO recording feature should be separated
) {
    private val nitronParser: NitronParser = config.parserConfig.getParser()
    val nodeTypePool: NodeTypePool = nitronParser.nodeTypes
    private val splitter = ThreadLocal.withInitial {
        AstSplitter(config.processConfig.splitConfig.splitRules.mapNotNull { nodeTypePool.getType(it) })
    }
    private val normalizer = ThreadLocal.withInitial {
        AstNormalizer(
            mapping = config.processConfig.normalizeConfig.mapping.entries.associate { (path, symbol) ->
                AstPath.of(path, nodeTypePool) to symbol
            },
            numberedMapping = config.processConfig.normalizeConfig.indexedMapping.entries.associate { (path, symbol) ->
                AstPath.of(path, nodeTypePool) to symbol
            },
            ignoreRules = config.processConfig.normalizeConfig.ignoreRules.map {
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
        return nitronParser.parse(input.reader().buffered())
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
