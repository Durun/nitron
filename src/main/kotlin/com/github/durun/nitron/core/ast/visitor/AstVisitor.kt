package com.github.durun.nitron.core.ast.visitor

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode

/**
 * [AstNode]にacceptさせると[R]を返すビジター
 */
interface AstVisitor<R> {
    fun visit(node: AstNode): R
    fun visitRule(node: AstRuleNode): R
    fun visitTerminal(node: AstTerminalNode): R
}