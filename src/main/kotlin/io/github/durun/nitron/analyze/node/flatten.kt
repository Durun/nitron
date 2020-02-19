package io.github.durun.nitron.analyze.node

import io.github.durun.nitron.inout.model.ast.SerializableAst
import io.github.durun.nitron.inout.model.ast.Visitor

fun SerializableAst.Node.flatten(): List<SerializableAst.Node> {
    return this.accept(FlattenVisitor)
}

private object FlattenVisitor: Visitor<List<SerializableAst.Node>> {
    override fun visitTerminal(node: SerializableAst.TerminalNode): List<SerializableAst.Node> {
        return listOf(node)
    }

    override fun visitRule(node: SerializableAst.RuleNode): List<SerializableAst.Node> {
        return node.children.flatMap { it.accept(this) }
    }

    override fun visitNormalizedRule(node: SerializableAst.NormalizedRuleNode): List<SerializableAst.Node> {
        return listOf(node)
    }

    override fun visitNodeList(node: SerializableAst.NodeList): List<SerializableAst.Node> {
        return node.children.flatMap { it.accept(this) }
    }

}