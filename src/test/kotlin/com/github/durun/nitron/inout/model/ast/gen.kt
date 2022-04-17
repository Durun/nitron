package com.github.durun.nitron.inout.model.ast

import com.github.durun.nitron.core.ast.node.ruleNode
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.ast.type.nodeTypePool
import com.github.durun.nitron.core.toMD5
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.take

fun Arb.Companion.structure(typeSet: NodeTypePool? = null): Arb<Structure> {
	return arbitrary { rs ->
		val types = typeSet ?: nodeTypePool().sample(rs).value
		Structure(
				nodeTypePool = types,
				ast = ruleNode(types).sample(rs).value,
				hash = byte().take(16, rs).toList().toMD5()
		)
	}
}