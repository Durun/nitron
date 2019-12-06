package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.visitor.AstVisitor

/**
 * 構文木の終端ノード
 */
class AstTerminalNode(
        /**
         * トークン
         */
        token: String,

        /**
         * 終端規則
         */
        val tokenType: String,

        /**
         * 元のソースコードとの対応位置
         */
        val line: Int
) : AstNode {
    var token: String = token
        private set

    fun replaceToken(newToken: String): AstTerminalNode {
        this.token = newToken
        return this
    }

    override val children: List<AstNode>?
        get() = null

    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitTerminal(this)
    override fun getText(): String = token
}