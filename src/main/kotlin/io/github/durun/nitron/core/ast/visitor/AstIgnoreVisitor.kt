package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.basic.*

class AstIgnoreVisitor(
        private val ignoreRules: List<String>
) : AstVisitor<AstNode> {
    override fun visit(node: AstNode): AstNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitRule(node: AstRuleNode): AstNode {
        return if (shouldIgnore(node))
            IgnoredAstNode()
        else {
            val children = node.children?.map { it.accept(this) }
            if (children.isNullOrEmpty() || children.all { it is IgnoredAstNode }) IgnoredAstNode()
            else BasicAstRuleNode(node.ruleName, children)
        }
    }

    override fun visitTerminal(node: AstTerminalNode): AstNode {
        return if (shouldIgnore(node))
            IgnoredAstNode()
        else
            node
    }

    private fun shouldIgnore(node: AstRuleNode) = ignoreRules.contains(node.ruleName)
    private fun shouldIgnore(node: AstTerminalNode) = ignoreRules.contains(node.tokenType)
}