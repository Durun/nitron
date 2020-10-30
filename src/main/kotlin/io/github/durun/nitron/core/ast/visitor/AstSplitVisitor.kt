package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.InvalidTypeException
import io.github.durun.nitron.core.ast.node.*

fun astSplitVisitorOf(splitTypes: List<String>): AstSplitVisitor {
    return StringAstSplitVisitor(splitTypes)
}

fun astSplitVisitorOf(types: NodeTypePool, splitTypes: List<String>): AstSplitVisitor {
    return FastAstSplitVisitor(types, splitTypes)
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

private class FastAstSplitVisitor(
        private val splitRules: Set<NodeType>
) : AstSplitVisitor() {
    constructor(types: NodeTypePool, splitRules: List<String>) : this(types.let { types ->
        val invalidTypes = splitRules.filter { types.getType(it) == null }
        if (invalidTypes.isNotEmpty()) {
            throw InvalidTypeException(invalidTypes)
        }
        types.filterRulesAndTokenTypes(splitRules)
    })
    constructor(types: NodeTypePool) : this(types.allTypes)

    override fun hasSplitRule(node: AstNode?): Boolean {
        return node is AstRuleNode &&
                splitRules.contains(node.type)
    }
}