package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.ast.type.TokenType
import com.github.durun.nitron.core.ast.visitor.AstVisitor
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * 構文木の終端ノード
 */
@Serializable
@SerialName("t")
class AstTerminalNode(
        /**
         * トークン
         */
        @SerialName("s")
        var token: String,

        /**
         * 終端規則
         */
        @Contextual
        @SerialName("t")
        override val type: TokenType,

        /**
         * 元のソースコードとの対応位置
         */
        @SerialName("l")
        val line: Int
) : AstNode {
    companion object {
        fun of(token: String, type: TokenType, line: Int, originalNode: AstTerminalNode): AstTerminalNode {
            return AstTerminalNode(token, type, line)
                .also { it.originalNode = originalNode.originalNode }
        }
    }

    @Transient
    override var originalNode: AstTerminalNode = this
        private set

    fun replaceToken(newToken: String): AstTerminalNode {
        this.token = newToken
        return this
    }

    override val children: List<AstNode>?
        get() = null

    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitTerminal(this)
    override fun getText(): String = token

    override fun copy(): AstNode = of(token, type, line, originalNode = this.originalNode)

    fun copy(line: Int): AstNode = of(token, type, line, originalNode = this.originalNode)

    override fun toString(): String = getText()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AstTerminalNode

        if (token != other.token) return false
        if (type != other.type) return false
        if (line != other.line) return false

        return true
    }

    override fun hashCode(): Int {
        var result = token.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + line
        return result
    }
}