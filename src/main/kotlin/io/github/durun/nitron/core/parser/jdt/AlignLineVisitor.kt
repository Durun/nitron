package io.github.durun.nitron.core.parser.jdt

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.visitor.AstVisitor

class AlignLineVisitor : AstVisitor<AstNode> {
    private var currentLine = 0
    override fun visitRule(node: AstRuleNode): AstNode = visit(node)
    override fun visit(node: AstNode): AstNode {
        return if (node is BasicAstRuleNode) {
            val newChildren = node.children.map { it.accept(this) }
            node.children.clear()
            node.children.addAll(newChildren)
            node
        } else node
    }

    override fun visitTerminal(node: AstTerminalNode): AstNode {
        return if (currentLine <= node.line) {
            currentLine = node.line
            node
        } else {
            node.copy(currentLine)
        }
    }
}