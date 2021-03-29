package io.github.durun.nitron.core.ast.processors

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.path.AstPath
import io.github.durun.nitron.core.ast.type.NodeType

class AstSplitter(
    private val paths: Collection<AstPath>,
    private val types: Collection<NodeType>
) : AstProcessor<List<AstNode>> {
    override fun process(ast: AstNode): List<AstNode> {
        val copied = ast.copy()
        val selected = paths.flatMap { it.select(copied) }
        return split(copied, selected)
    }

    private fun AstNode.isSelectedBy(selected: Collection<AstNode>): Boolean {
        return selected.any { this === it }
    }

    private fun AstNode.isSelectedBy(): Boolean {
        return types.any { it == type }
    }

    private fun split(ast: AstNode, selected: Collection<AstNode>): List<AstNode> {
        if (ast is AstTerminalNode) return listOf(ast)
        val children = ast.children?.flatMap { split(it, selected) }
        val buf: MutableList<MutableList<AstNode>> = mutableListOf(mutableListOf())
        children?.forEach {
            if (it.isSelectedBy()) buf.add(mutableListOf())
            buf.last().add(it)
            if (it.isSelectedBy()) buf.add(mutableListOf())
        }
        return buf
            .filter { it.isNotEmpty() }
            .map { newChildren ->
                if (newChildren.first().isSelectedBy()) newChildren.first()
                else when (ast) {
                    is BasicAstRuleNode -> BasicAstRuleNode(ast.type, newChildren.toMutableList())
                    else -> ast
                }
            }
    }
}