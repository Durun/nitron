package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.ast.type.RuleType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("r")
class BasicAstRuleNode(
    @Contextual
        @SerialName("t")
        override val type: RuleType,

    @SerialName("c")
    override val children: MutableList<AstNode>
) : AstRuleNode {
    companion object {
        fun of(type: RuleType, children: MutableList<AstNode>, originalNode: BasicAstRuleNode): BasicAstRuleNode {
            return BasicAstRuleNode(type, children)
                .also { it.originalNode = originalNode.originalNode }
        }
    }

    @Transient
    override var parent: AstNode? = null
        private set

    @Transient  // exclude from serialization
    override var originalNode: BasicAstRuleNode = this
        private set

    override fun getText(): String {
        return children.joinToString(" ") { it.getText() }
    }

    override fun copy() = of(type, children.map { it.copy() }.toMutableList(), originalNode = this.originalNode)

    override fun toString(): String = getText()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BasicAstRuleNode

        if (type != other.type) return false
        if (children != other.children) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }
}