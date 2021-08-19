package com.github.durun.nitron.core.parser.jdt

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import com.github.durun.nitron.core.ast.visitor.AstVisitor

class AlignLineVisitor : AstVisitor<AstNode> {
    private var currentLine = 1
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