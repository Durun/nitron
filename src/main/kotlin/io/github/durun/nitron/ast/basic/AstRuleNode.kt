package io.github.durun.nitron.ast.basic

import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.AstVisitor

interface AstRuleNode: AstNode {
    val ruleName: String

    override fun <R> accept(visitor: AstVisitor<R>) = visitor.visitRule(this)
}