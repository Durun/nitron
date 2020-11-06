package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode

data class AstStats(
		val size: Int
)

object AstCountVisitor: AstVisitor<AstStats> {
	override fun visit(node: AstNode): AstStats {
		val children = node.children.orEmpty().map { it.accept(this) }
		return AstStats(
				size = children.sumBy { it.size } + 1
		)
	}

	override fun visitRule(node: AstRuleNode): AstStats = visit(node)
	override fun visitTerminal(node: AstTerminalNode): AstStats = visit(node)
}