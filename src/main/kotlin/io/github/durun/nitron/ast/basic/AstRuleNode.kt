package io.github.durun.nitron.ast.basic

import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.AstVisitor

/**
 * 構文木の非終端ノード
 */
interface AstRuleNode: AstNode {
    /**
     * 非終端規則
     */
    val ruleName: String

    override fun <R> accept(visitor: AstVisitor<R>) = visitor.visitRule(this)
}