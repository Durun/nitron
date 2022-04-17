package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.NitronException
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

    override val children: List<AstNode>
        get() = childrenList

    @Transient
    override var parent: AstNode? = null
        private set

    @Transient  // exclude from serialization
    override var originalNode: BasicAstRuleNode = this
        private set


    fun addChildren(children: Collection<AstNode>): Boolean {
        children.forEach {
            it.checkParentIs(null)
            it.setParent(this)
        }
        return childrenList.addAll(children)
    }

    fun addChild(child: AstNode): Boolean {
        child.checkParentIs(null)
        child.setParent(this)
        return childrenList.add(child)
    }

    fun setChild(index: Int, child: AstNode): AstNode {
        child.checkParentIs(null)
        child.setParent(this)
        val oldChild = childrenList.set(index, child)
        oldChild.setParent(null)
        return oldChild
    }

    fun clearChildren() {
        childrenList.forEach { it.setParent(null) }
        childrenList.clear()
    }

    fun removeChildAt(index: Int): AstNode {
        val oldChild = childrenList.removeAt(index)
        oldChild.setParent(null)
        return oldChild
    }

    fun removeChildIf(filter: Predicate<in AstNode>): Boolean {
        val iter = childrenList.iterator()
        var removed = false
        while (iter.hasNext()) {
            val e = iter.next()
            if (filter.test(e)) {
                iter.remove()
                e.setParent(null)
                removed = true
            }
        }
        return removed
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

    private fun AstNode.setParent(newParent: AstNode?) {
        when (this) {
            is AstTerminalNode -> {
                parent = newParent
            }
            is BasicAstRuleNode -> {
                parent = newParent
            }
            is NormalAstRuleNode -> {
                parent = newParent
            }
        }
    }

    private fun AstNode.checkParentIs(parent: AstNode?) {
        if (this.parent != parent) throw NitronException("Illegal state: Parent of $this must be $parent but ${this.parent}")
    }
}