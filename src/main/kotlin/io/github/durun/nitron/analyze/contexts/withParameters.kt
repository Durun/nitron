package io.github.durun.nitron.analyze.contexts

import io.github.durun.nitron.analyze.node.filterOnce
import io.github.durun.nitron.analyze.node.flatten
import io.github.durun.nitron.core.ast.node.NodeType
import io.github.durun.nitron.inout.model.ast.SerializableAst
import io.github.durun.nitron.inout.model.ast.Visitor

fun SerializableAst.Node.typeWithParameters(type: NodeType, parameterTypes: List<NodeType>): Sequence<List<SerializableAst.Node>> {
    val roots = this.filterOnce { it.type == type.index }
    return roots.mapNotNull { it.collectParameters(parameterTypes) }
}

fun SerializableAst.Node.collectParameters(parameterTypes: List<NodeType>): List<SerializableAst.Node>? {
    val parameters = this.accept(ParameterCollectVisitor(parameterTypes))
    return parameters.takeIf { it.size == parameterTypes.size }?.flatten()
}

private class ParameterCollectVisitor(
        private val parameterTypes: Iterable<NodeType>
) : Visitor<List<List<SerializableAst.Node>>> {
    constructor(parameterTypes: Array<out NodeType>) : this(parameterTypes.asIterable())

    override fun visitTerminal(node: SerializableAst.TerminalNode): List<List<SerializableAst.Node>> {
        return visitLeaf(node)
    }

    override fun visitRule(node: SerializableAst.RuleNode): List<List<SerializableAst.Node>> {
        val text = textOf(node) ?: return visitChildren(node)
        return listOf(text)
    }

    override fun visitNormalizedRule(node: SerializableAst.NormalizedRuleNode): List<List<SerializableAst.Node>> {
        return visitLeaf(node)
    }

    override fun visitNodeList(node: SerializableAst.NodeList): List<List<SerializableAst.Node>> {
        return visitChildren(node)
    }

    private fun visitLeaf(node: SerializableAst.Node): List<List<SerializableAst.Node>> {
        val text = textOf(node) ?: return emptyList()
        return listOf(text)
    }

    private fun visitChildren(node: SerializableAst.NonTerminalNode): List<List<SerializableAst.Node>> {
        return node.children.fold(emptyList()) { list, it ->
            val newVisitor = if (list.isNotEmpty()) ParameterCollectVisitor(parameterTypes.drop(list.size)) else this
            list + it.accept(newVisitor)
        }
    }

    private fun textOf(node: SerializableAst.Node): List<SerializableAst.Node>? {
        val type = parameterTypes.firstOrNull() ?: return null
        return node.takeIf { it.type == type.index }?.flatten()
    }
}