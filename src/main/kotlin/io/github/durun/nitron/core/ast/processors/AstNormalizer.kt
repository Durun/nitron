package io.github.durun.nitron.core.ast.processors

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.core.ast.path.AstPath

class AstNormalizer(
	private val mapping: Map<AstPath, String>,
	private val numberedMapping: Map<AstPath, String>,
	private val ignoreRules: Collection<AstPath>
) : AstProcessor<AstNode?> {
	override fun process(ast: AstNode): AstNode? {
		var copied: AstNode? = ast.copy()
		ignoreRules.forEach {
			copied = it.removeNode(copied ?: return null)
		}
		mapping.entries.forEach { (path, symbol) ->
			copied = path.replaceNode(root = copied ?: return null) {
				it.normalizeTo(symbol)
			}
		}
		val nameTables: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
		numberedMapping.entries.forEach { (path, symbol) ->
			copied = path.replaceNode(root = copied ?: return null) {
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