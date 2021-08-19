package com.github.durun.nitron.core.ast.visitor

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode

/**
 * Line numberの範囲を求めるビジター。
 * ただし、正規化する前のAstNodeにacceptさせなければならない。
 */
object AstLineGetVisitor : AstVisitor<LineRange> {
    override fun visit(node: AstNode): LineRange {
        return LineRange(
            getFirstLine(node),
            getLastLine(node)
        )
    }

    override fun visitRule(node: AstRuleNode): LineRange {
        return LineRange(
            getFirstLine(node),
            getLastLine(node)
        )
    }

    override fun visitTerminal(node: AstTerminalNode): LineRange {
        return LineRange(node.line, node.line)
    }

    private fun getFirstLine(node: AstNode): Int {
        var current = node
        while (current !is AstTerminalNode) {
            current = current.children?.first()
                ?: throw Exception("Can't get line number from normalized AstNode")
        }
        return current.line
    }

    private fun getLastLine(node: AstNode): Int {
        var current = node
        while (current !is AstTerminalNode) {
            current = current.children?.last()
                ?: throw Exception("Can't get line number from normalized AstNode")
        }
        return current.line
    }
}

data class LineRange(val first: Int, val last: Int)