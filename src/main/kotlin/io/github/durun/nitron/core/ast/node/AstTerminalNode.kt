package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.type.TokenType
import io.github.durun.nitron.core.ast.visitor.AstVisitor
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @Deprecated("use type", ReplaceWith("type.name"))
    val tokenType: String
        get() = type.name

    fun replaceToken(newToken: String): AstTerminalNode {
        this.token = newToken
        return this
    }

    override val children: List<AstNode>?
        get() = null

    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitTerminal(this)
    override fun getText(): String = token

    override fun copy(): AstNode = AstTerminalNode(token, type, line)
    fun copy(line: Int): AstNode = AstTerminalNode(token, type, line)

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