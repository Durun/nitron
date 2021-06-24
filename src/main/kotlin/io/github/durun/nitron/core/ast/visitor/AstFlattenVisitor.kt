package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode

fun AstNode.flatten(): List<AstNode> = accept(AstFlattenVisitor)

private object AstFlattenVisitor : AstVisitor<List<AstNode>> {
    override fun visitTerminal(node: AstTerminalNode): List<AstNode> = listOf(node)
    override fun visitRule(node: AstRuleNode): List<AstNode> = visit(node)
    override fun visit(node: AstNode): List<AstNode> =
        node.children?.takeIf { it.isNotEmpty() }?.flatMap { it.accept(this) } ?: listOf(node)
}