package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.AstVisitor
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode

object AstFlattenVisitor : AstVisitor<List<AstTerminalNode>> {
    override fun visit(node: AstNode): List<AstTerminalNode> {
        return node.children?.flatMap {
            it.accept(this)
        } ?: emptyList()
    }

    override fun visitRule(node: AstRuleNode): List<AstTerminalNode> {
        return visit(node)
    }

    override fun visitTerminal(node: AstTerminalNode): List<AstTerminalNode> {
        return listOf(node)
    }
}