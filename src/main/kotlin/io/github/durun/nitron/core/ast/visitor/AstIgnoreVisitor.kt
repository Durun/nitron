package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.*

fun astIgnoreVisitorOf(ignoreTypes: List<String>): AstIgnoreVisitor {
    return StringAstIgnoreVisitor(ignoreTypes)
}

fun astIgnoreVisitorOf(types: NodeTypePool, ignoreTypes: List<String>): AstIgnoreVisitor {
    return FastAstIgnoreVisitor(types, ignoreTypes)
}

abstract class AstIgnoreVisitor : AstVisitor<AstNode?> {
    protected abstract fun shouldIgnore(node: AstRuleNode): Boolean
    protected abstract fun shouldIgnore(node: AstTerminalNode): Boolean

    override fun visit(node: AstNode): AstNode? = TODO()

    override fun visitRule(node: AstRuleNode): AstNode? {
        return if (shouldIgnore(node))
            null
        else {
            val children = node.children?.mapNotNull { it.accept(this) }
            if (children.isNullOrEmpty()) null
            else node.replaceChildren(children)
        }
    }

    override fun visitTerminal(node: AstTerminalNode): AstNode? {
        return if (shouldIgnore(node))
            null
        else
            node
    }
}

private class StringAstIgnoreVisitor(
        private val ignoreRules: List<String>
) : AstIgnoreVisitor() {
    override fun shouldIgnore(node: AstRuleNode) = ignoreRules.contains(node.type.name)
    override fun shouldIgnore(node: AstTerminalNode) = ignoreRules.contains(node.type.name)
}

private class FastAstIgnoreVisitor(
        private val ignoreRules: Set<NodeType>
) : AstIgnoreVisitor() {
    constructor(types: NodeTypePool, ignoreTypes: List<String>) : this(types.filterRulesAndTokenTypes(ignoreTypes))
    constructor(types: NodeTypePool) : this(types.allTypes)

    override fun shouldIgnore(node: AstRuleNode) = ignoreRules.contains(node.type)
    override fun shouldIgnore(node: AstTerminalNode) = ignoreRules.contains(node.type)
}
