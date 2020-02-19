package io.github.durun.nitron.analyze.node

import io.github.durun.nitron.core.ast.node.NodeType
import io.github.durun.nitron.inout.model.ast.SerializableAst

fun SerializableAst.Node.hasNestOf(conditions: List<(SerializableAst.Node) -> Boolean>): Boolean {
    val cond = conditions.firstOrNull() ?: return true
    val remain = this.filterOnce(cond)
    return remain.any { it.hasNestOf(conditions.drop(1)) }
}

fun SerializableAst.Node.hasNestTypeOf(vararg types: NodeType): Boolean {
    return hasNestOf(types.map { { node: SerializableAst.Node -> node.type == it.index } })
}