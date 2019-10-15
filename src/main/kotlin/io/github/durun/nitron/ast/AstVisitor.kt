package io.github.durun.nitron.ast

import io.github.durun.nitron.ast.basic.AstTerminalNode
import io.github.durun.nitron.ast.basic.AstRuleNode

interface AstVisitor<R> {
    fun visit(node: AstNode): R
    fun visitRule(node: AstRuleNode): R
    fun visitTerminal(node: AstTerminalNode): R
}