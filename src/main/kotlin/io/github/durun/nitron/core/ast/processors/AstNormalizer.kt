package io.github.durun.nitron.core.ast.processors

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.core.ast.path.AstPath

class AstNormalizer(
	private val mapping: Map<AstPath, String>,
	private val numberedMapping: Map<AstPath, String>
) : AstProcessor<AstNode> {
	override fun process(ast: AstNode): AstNode {
		val copied = ast.copy()
		mapping.entries.forEach { (path, symbol) ->
			path.replaceNode(root = copied) {
				when (it) {
					is AstRuleNode -> NormalAstRuleNode(it.type, symbol)
					is AstTerminalNode -> it.replaceToken(symbol)
					else -> throw IllegalStateException()
				}
			}
		}
		val count: MutableMap<String, Int> = mutableMapOf()
		numberedMapping.entries.forEach { (path, symbol) ->
			path.replaceNode(root = copied) {
				val n = count[symbol] ?: 0
				count[symbol] = n + 1
				when (it) {
					is AstRuleNode -> NormalAstRuleNode(it.type, "$symbol$n")
					is AstTerminalNode -> it.replaceToken("$symbol$n")
					else -> throw IllegalStateException()
				}
			}
		}
		return copied
	}
}