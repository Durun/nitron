package io.github.durun.nitron.analyze.node

import io.github.durun.nitron.inout.model.ast.SerializableAst
import io.github.durun.nitron.inout.model.ast.Visitor


fun SerializableAst.Node.filterOnce(cond: (SerializableAst.Node) -> Boolean): Sequence<SerializableAst.Node> {
    return this.accept(FilterOnceVisitor(cond))
}

private class FilterOnceVisitor(
        val cond: (SerializableAst.Node) -> Boolean
) : Visitor<Sequence<SerializableAst.Node>> {
    override fun visitTerminal(node: SerializableAst.TerminalNode): Sequence<SerializableAst.Node> {
        return visitLeaf(node)
    }

    override fun visitRule(node: SerializableAst.RuleNode): Sequence<SerializableAst.Node> {
        return if (cond(node))
            visitLeaf(node)                 // stop
        else
            visitIterable(node.children)    // continue
    }

    override fun visitNormalizedRule(node: SerializableAst.NormalizedRuleNode): Sequence<SerializableAst.Node> {
        return visitLeaf(node)
    }

    override fun visitNodeList(node: SerializableAst.NodeList): Sequence<SerializableAst.Node> {
        return visitIterable(node.children)
    }

    private fun visitLeaf(node: SerializableAst.Node): Sequence<SerializableAst.Node> {
        return if (cond(node)) sequenceOf(node) else emptySequence()
    }

    private fun visitIterable(nodes: Iterable<SerializableAst.Node>): Sequence<SerializableAst.Node> {
        return nodes.asSequence().flatMap { it.accept(this) }
    }
}