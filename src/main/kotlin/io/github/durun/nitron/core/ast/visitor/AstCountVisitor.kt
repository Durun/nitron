package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode

data class AstStats(
		val size: Int
)

object AstCountVisitor: AstVisitor<AstStats> {
	override fun visit(node: AstNode): AstStats {
		TODO("Not yet implemented")
	}

	override fun visitRule(node: AstRuleNode): AstStats {
		TODO("Not yet implemented")
	}

	override fun visitTerminal(node: AstTerminalNode): AstStats {
		TODO("Not yet implemented")
	}
}