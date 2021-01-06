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
        override val type: RuleType,
        private val text: String? = null
) : AstRuleNode {
    /**
     *  @param [originalNode] 元の非終端ノード
     */
    constructor(originalNode: AstRuleNode, text: String? = null) : this(
            type = originalNode.type,
            text = text
    )

    override val children: List<AstNode>?
        get() = null

    override fun getText(): String = text ?: type.name.toUpperCase()

    override fun replaceChildren(newChildren: List<AstNode>): AstRuleNode {
        return this
    }

    override fun copyWithChildren(children: List<AstNode>): AstRuleNode {
        return this
    }

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

