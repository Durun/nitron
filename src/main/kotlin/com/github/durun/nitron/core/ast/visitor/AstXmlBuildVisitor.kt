package com.github.durun.nitron.core.ast.visitor

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import org.apache.commons.text.StringEscapeUtils

object AstXmlBuildVisitor : AstVisitor<String> {
    override fun visit(node: AstNode): String {
        TODO("Not yet implemented")
    }

    override fun visitRule(node: AstRuleNode): String {
        val tag = node.type.name
        val children = node.children.orEmpty().joinToString("") { it.accept(this) }
        return if (children.isNotEmpty())
            """<$tag tag="$tag">${children}</$tag>"""
        else
            """<$tag tag="$tag">${node.getText()}</$tag>"""
    }

    override fun visitTerminal(node: AstTerminalNode): String {
        val tag = node.type.name
        val value = StringEscapeUtils.escapeXml10(node.token)
        return """<$tag tag="$tag">$value</$tag>"""
    }
}