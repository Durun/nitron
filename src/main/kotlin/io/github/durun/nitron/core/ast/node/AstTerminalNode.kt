package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.visitor.AstVisitor

/**
 * 構文木の終端ノード
 */
class AstTerminalNode(
        /**
         * トークン
         */
        val token: String,

        /**
         * 終端規則
         */
        val tokenType: String,
        override val range: TextRange
) : AstNode {
    override val children: List<AstNode>?
        get() = null

    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitTerminal(this)
    override fun getText(): String = token
}