package com.github.durun.nitron.core.ast.processors

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import com.github.durun.nitron.core.ast.type.NodeType

class AstSplitter(
    private val types: Collection<NodeType>
) : AstProcessor<List<AstNode>> {
    override fun process(ast: AstNode): List<AstNode> {
        val copied = ast.copy()
        return split(copied)
    }

    private fun AstNode.isSelectedBy(): Boolean {
        return (this is AstRuleNode) && types.any { it == type }
    }

    private fun split(ast: AstNode): List<AstNode> = when (ast) {
        is BasicAstRuleNode -> {
            val children = ast.children.flatMap { split(it) }
            val buf: MutableList<MutableList<AstNode>> = mutableListOf(mutableListOf())
            children.forEach {
                if (it.isSelectedBy()) buf.add(mutableListOf())
                buf.last().add(it)
                if (it.isSelectedBy()) buf.add(mutableListOf())
            }
            buf.filter { it.isNotEmpty() }
                .map { newChildren ->
                    if (newChildren.first().isSelectedBy()) newChildren.first()
                    else BasicAstRuleNode(ast.type, newChildren.toMutableList())
                }
        }
        else -> listOf(ast)
    }
}