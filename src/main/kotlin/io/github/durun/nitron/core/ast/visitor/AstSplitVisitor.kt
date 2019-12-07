package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.*

fun astSplitVisitorOf(splitRules: List<String>): AstSplitVisitor {
    return StringAstSplitVisitor(splitRules)
}

abstract class AstSplitVisitor : AstVisitor<List<AstNode>> {
    protected abstract fun hasSplitRule(node: AstNode?): Boolean

    override fun visit(node: AstNode): List<AstNode> {
        return listOf(node)
    }

    override fun visitRule(node: AstRuleNode): List<AstNode> {
        val children = node.children?.flatMap { it.accept(this) }
        val buf = mutableListOf(mutableListOf<AstNode>())
        children?.forEach {
            if (hasSplitRule(it)) buf.add(mutableListOf())
            buf.last().add(it)
            if (hasSplitRule(it)) buf.add(mutableListOf())
        }
        return buf
                .filter { it.isNotEmpty() }
                .map { newChildren ->
                    if (hasSplitRule(newChildren.firstOrNull())) newChildren.first()
                    else node.copyWithChildren(newChildren)
                }
    }

    override fun visitTerminal(node: AstTerminalNode): List<AstNode> {
        return listOf(node)
    }
}

private class StringAstSplitVisitor(
        private val splitRules: List<String>
) : AstSplitVisitor() {
    override fun hasSplitRule(node: AstNode?): Boolean {
        return node is AstRuleNode &&
                splitRules.contains(node.type.name)
    }
}