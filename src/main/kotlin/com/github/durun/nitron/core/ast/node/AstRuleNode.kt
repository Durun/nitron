package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.visitor.AstVisitor

/**
 * 構文木の非終端ノード
 */
interface AstRuleNode : AstNode {

    override val type: RuleType

    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitRule(this)

    override fun copy(): AstRuleNode
}