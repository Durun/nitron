package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.visitor.AstVisitor

/**
 * 構文木の非終端ノード
 */
interface AstRuleNode : AstNode {
    /**
     * 非終端規則
     */
    @Deprecated("use type", ReplaceWith("type.name"))
    val ruleName: String
        get() = type.name

    override val type: RuleType

    fun replaceChildren(newChildren: List<AstNode>): AstRuleNode
    fun copyWithChildren(children: List<AstNode>): AstRuleNode

    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitRule(this)
}