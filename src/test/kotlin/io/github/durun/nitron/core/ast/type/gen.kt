package io.github.durun.nitron.core.ast.type

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.string

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