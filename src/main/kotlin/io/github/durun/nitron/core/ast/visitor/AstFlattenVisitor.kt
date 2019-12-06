package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode

object AstFlattenVisitor : AstVisitor<Sequence<AstTerminalNode>> {
    override fun visit(node: AstNode): Sequence<AstTerminalNode> {
        return node.children?.asSequence()?.flatMap {
            it.accept(this)
        } ?: emptySequence()
    }

    override fun visitRule(node: AstRuleNode): Sequence<AstTerminalNode> {
        return visit(node)
    }

    override fun visitTerminal(node: AstTerminalNode): Sequence<AstTerminalNode> {
        return sequenceOf(node)
    }
}