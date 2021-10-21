package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.ast.type.RuleType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

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
    companion object {
        fun of(type: RuleType, text: String?, originalNode: NormalAstRuleNode): NormalAstRuleNode {
            return NormalAstRuleNode(type, text)
                .also { it.originalNode = originalNode.originalNode }
        }
    }

    override val children: List<AstNode>?
        get() = null

    override var originalNode: NormalAstRuleNode = this
        private set

    override fun getText(): String = text ?: type.name.uppercase(Locale.getDefault())

    override fun copy() = of(type, text, originalNode = this.originalNode)

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

