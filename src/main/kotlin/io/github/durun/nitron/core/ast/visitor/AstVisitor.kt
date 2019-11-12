package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.basic.AstNode
import io.github.durun.nitron.core.ast.basic.AstRuleNode
import io.github.durun.nitron.core.ast.basic.AstTerminalNode

/**
 * [AstNode]にacceptさせると[R]を返すビジター
 */
interface AstVisitor<R> {
    fun visit(node: AstNode): R
    fun visitRule(node: AstRuleNode): R
    fun visitTerminal(node: AstTerminalNode): R
}