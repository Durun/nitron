package io.github.durun.nitron.core.ast.basic

import io.github.durun.nitron.core.ast.AstNode
import io.github.durun.nitron.core.ast.AstVisitor

/**
 * 構文木の非終端ノード
 */
interface AstRuleNode : AstNode {
    /**
     * 非終端規則
     */
    val ruleName: String

    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitRule(this)
}