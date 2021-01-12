package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.ast.node.ruleNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.type.nodeTypePool
import io.github.durun.nitron.core.toMD5
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.take

fun Arb.Companion.structure(typeSet: NodeTypePool? = null): Arb<Structure> {
	return arbitrary { rs ->
		val typeSet = typeSet ?: nodeTypePool().sample(rs).value
		Structure(
				nodeTypePool = typeSet,
				ast = ruleNode(typeSet).sample(rs).value,
				hash = byte().take(16, rs).toList().toMD5()
		)
	}
}