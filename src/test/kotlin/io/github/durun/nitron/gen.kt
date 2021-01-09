package io.github.durun.nitron

import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.type.RuleType
import io.github.durun.nitron.core.ast.type.TokenType
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*

fun Arb.Companion.nodeTypePool(tokenTypes: Int = 4, ruleTypes: Int = 6): Arb<NodeTypePool> {
	require(1 <= tokenTypes)
	require(1 <= ruleTypes)
	return arbitrary { rs ->
		val names = string(minSize = 1, maxSize = 15)
				.samples(rs)
				.distinct()
				.take(tokenTypes + ruleTypes)
				.mapIndexed { index, it -> index to it.value }
				.toList()
		val tokens = names.takeLast(tokenTypes).map { (index, name) -> TokenType(index, name) }
		val rules = names.take(ruleTypes).map { (index, name) -> RuleType(index, name) }
		NodeTypePool.of(
				grammarName = string(minSize = 1, maxSize = 10).sample(rs).value,
				tokenTypes = tokens,
				ruleTypes = rules
		)
	}
}

fun Arb.Companion.terminalNode(typeSet: NodeTypePool, lineRange: IntRange? = null): Arb<AstTerminalNode> {
	return arbitrary { rs ->
		AstTerminalNode(
				token = string(minSize = 1, maxSize = 20).sample(rs).value,
				type = typeSet.tokenTypes.random(rs.random),
				line = lineRange?.random(rs.random) ?: int().sample(rs).value
		)
	}
}

fun Arb.Companion.ruleNode(typeSet: NodeTypePool, maxDepth: Int = 5, maxWidth: Int = 10): Arb<BasicAstRuleNode> {
	require(2 <= maxDepth)
	require(1 <= maxWidth)
	val child = if (maxDepth == 2)
		terminalNode(typeSet)
	else
		choice(
				terminalNode(typeSet),
				ruleNode(typeSet, maxDepth - 1, maxWidth)
		)
	return arbitrary { rs ->
		BasicAstRuleNode(
				type = typeSet.ruleTypes.random(rs.random),
				children = list(gen = child, range = 1..maxWidth).sample(rs).value
		)
	}
}