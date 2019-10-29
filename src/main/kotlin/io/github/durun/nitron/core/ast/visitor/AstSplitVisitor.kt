package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.AstNode
import io.github.durun.nitron.core.ast.AstVisitor
import io.github.durun.nitron.core.ast.basic.AstRuleNode
import io.github.durun.nitron.core.ast.basic.AstTerminalNode
import io.github.durun.nitron.core.ast.basic.BasicAstRuleNode

class AstSplitVisitor(
        private val splitRules: List<String>
) : AstVisitor<List<AstNode>> {
    override fun visit(node: AstNode): List<AstNode> {
        return emptyList()
    }

    override fun visitRule(node: AstRuleNode): List<AstNode> {
        val children = node.children?.flatMap { it.accept(this) }
        val buf = mutableListOf(mutableListOf<AstNode>())
        children?.forEach {
            if (hasSplitRule(it)) buf.add(mutableListOf())
            buf.last().add(it)
            if (hasSplitRule(it)) buf.add(mutableListOf())
        }
        return buf.map {
            if (hasSplitRule(it.firstOrNull())) it.first()
            else BasicAstRuleNode(node.ruleName, it)
        }
    }

    override fun visitTerminal(node: AstTerminalNode): List<AstNode> {
        return listOf(node)
    }

    private fun hasSplitRule(node: AstNode?): Boolean {
        return node is AstRuleNode &&
                splitRules.contains(node.ruleName)
    }
}