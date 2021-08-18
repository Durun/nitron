package com.github.durun.nitron.core.ast.visitor

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode

/**
 * Line numberの範囲を求めるビジター。
 * ただし、正規化する前のAstNodeにacceptさせなければならない。
 */
object AstLineGetVisitor : AstVisitor<IntRange> {
    override fun visit(node: AstNode): IntRange {
        TODO("Not yet implemented")
    }

    override fun visitRule(node: AstRuleNode): IntRange {
        val first = node.children?.first()
            ?: throw Exception("Can't get line number from normalized AstNode")
        val last = node.children?.last()
            ?: throw Exception("Can't get line number from normalized AstNode")
        val firstRange = first.accept(this)
        return if (first === last) {
            firstRange
        } else {
            val lastRange = last.accept(this)
            firstRange.first..lastRange.last
        }
    }

    override fun visitTerminal(node: AstTerminalNode): IntRange {
        return node.line..node.line
    }
}