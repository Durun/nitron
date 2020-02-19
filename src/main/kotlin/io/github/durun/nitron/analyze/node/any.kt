package io.github.durun.nitron.analyze.node

import io.github.durun.nitron.core.ast.node.NodeType
import io.github.durun.nitron.inout.model.ast.SerializableAst
import io.github.durun.nitron.inout.model.ast.Visitor

fun SerializableAst.Node.any(cond: (SerializableAst.Node) -> Boolean): Boolean {
    return this.accept(AnyVisitor(cond))
}

fun SerializableAst.Node.contains(type: NodeType): Boolean {
    return this.any { it.type == type.index }
}

private class AnyVisitor(
        val cond: (SerializableAst.Node) -> Boolean
) : Visitor<Boolean> {
    override fun visitTerminal(node: SerializableAst.TerminalNode): Boolean {
        return cond(node)
    }

    override fun visitRule(node: SerializableAst.RuleNode): Boolean {
        return cond(node) || node.children.any { it.accept(this) }
    }

    override fun visitNormalizedRule(node: SerializableAst.NormalizedRuleNode): Boolean {
        return cond(node)
    }

    override fun visitNodeList(node: SerializableAst.NodeList): Boolean {
        return node.children.any { it.accept(this) }
    }
}