package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.ast.type.NodeTypePool
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*


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
        BasicAstRuleNode.of(
            type = typeSet.ruleTypes.random(rs.random),
            children = list(gen = child, range = 1..maxWidth).sample(rs).value.toMutableList()
        )
    }
}