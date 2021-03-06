package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.type.RuleType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("r")
class BasicAstRuleNode(
    @Contextual
        @SerialName("t")
        override val type: RuleType,

    @SerialName("c")
    override val children: MutableList<AstNode>
) : AstRuleNode {
    override fun getText(): String {
        return children.joinToString(" ") { it.getText() }
    }

    override fun copy() = BasicAstRuleNode(type, children.map { it.copy() }.toMutableList())

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