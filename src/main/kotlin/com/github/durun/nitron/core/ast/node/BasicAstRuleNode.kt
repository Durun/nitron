package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.ast.type.RuleType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.function.Predicate

@Serializable
@SerialName("r")
class BasicAstRuleNode
private constructor(
    @Contextual
    @SerialName("t")
    override val type: RuleType,

    @SerialName("c")
    private val childrenList: MutableList<AstNode>
) : AstRuleNode {
    companion object {
        @JvmStatic
        fun of(
            type: RuleType,
            children: List<AstNode>,
            originalNode: BasicAstRuleNode? = null
        ): BasicAstRuleNode {
            val node = BasicAstRuleNode(type, mutableListOf())
            originalNode?.let { node.originalNode = it.originalNode }
            node.addChildren(children)
            return node
        }
    }

    @Transient
    override val children: List<AstNode>
        get() = childrenList

    @Transient
    override var parent: AstNode? = null
        private set

    @Transient  // exclude from serialization
    override var originalNode: BasicAstRuleNode = this
        private set


    fun addChildren(children: Collection<AstNode>): Boolean {
        return childrenList.addAll(children)
    }

    fun addChild(child: AstNode): Boolean {
        return childrenList.add(child)
    }

    fun setChild(index: Int, child: AstNode): AstNode {
        return childrenList.set(index, child)
    }

    fun clearChildren() {
        childrenList.clear()
    }

    fun removeChildAt(index: Int): AstNode {
        return childrenList.removeAt(index)
    }

    fun removeChildIf(filter: Predicate<in AstNode>): Boolean {
        return childrenList.removeIf(filter)
    }

    override fun getText(): String {
        return children.joinToString(" ") { it.getText() }
    }

    override fun copy() = of(type, children.map { it.copy() }, originalNode = this.originalNode)

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