package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.type.RuleType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 部分木の情報を除くことで抽象化された非終端ノード.
 */
@Serializable
@SerialName("n")
class NormalAstRuleNode(
        @Contextual
        @SerialName("t")
        override val type: RuleType,

        @SerialName("s")
        private val text: String? = null
) : AstRuleNode {
    override val children: List<AstNode>?
        get() = null

    override fun getText(): String = text ?: type.name.toUpperCase()

    override fun copy() = NormalAstRuleNode(type, text)

    override fun toString(): String = getText()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NormalAstRuleNode

        if (type != other.type) return false
        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        return result
    }
}

