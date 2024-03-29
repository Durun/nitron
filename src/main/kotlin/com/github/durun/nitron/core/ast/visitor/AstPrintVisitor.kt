package com.github.durun.nitron.core.ast.visitor

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode

object AstPrintVisitor
    : AstVisitor<String> {
    override fun visit(node: AstNode): String {
        return ""
    }

    override fun visitRule(node: AstRuleNode): String {
        val thisTokens = node.getText().split(" ").let {
            val n = 3
            it
                    .take(n)
                    .joinToString(" ") +
                    if (n < it.size) " ..." else ""
        }
        val thisText = "${node.type.name}\t\t$thisTokens"
        val childrenText = node.children.orEmpty()
                .joinToString("\n") { it.accept(this) }
                .prependIndent("\t")
        return thisText + "\n" + childrenText
    }

    override fun visitTerminal(node: AstTerminalNode): String {
        return "${node.type.name} ${node.getText()}"
    }
}