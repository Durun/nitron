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
				it.normalizeTo(symbol)
			}
		}
		val nameTables: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
		numberedMapping.entries.forEach { (path, symbol) ->
			path.replaceNode(root = copied) {
				val table = nameTables.computeIfAbsent(symbol) { mutableMapOf() }
				val name = it.getText()
				val n = table.computeIfAbsent(name) { table.size }
				it.normalizeTo("$symbol$n")
			}
		}
		return copied
	}

	private fun AstNode.normalizeTo(text: String): AstNode = when (this) {
		is AstRuleNode -> NormalAstRuleNode(type, text)
		is AstTerminalNode -> replaceToken(text)
		else -> throw IllegalStateException()
	}
}