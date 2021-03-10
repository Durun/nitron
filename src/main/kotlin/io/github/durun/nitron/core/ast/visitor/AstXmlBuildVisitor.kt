package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode

object AstXmlBuildVisitor : AstVisitor<String> {
    override fun visit(node: AstNode): String {
        TODO("Not yet implemented")
    }

    override fun visitRule(node: AstRuleNode): String {
        val tag = node.type.name
        val children = node.children.orEmpty().joinToString("") { it.accept(this) }
        return if (children.isNotEmpty()) "<$tag>${children}</$tag>"
        else "<$tag>${node.getText()}</$tag>"
    }

    override fun visitTerminal(node: AstTerminalNode): String {
        val tag = node.type.name
        return "<$tag>${node.token}</$tag>"
    }
}