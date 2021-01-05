package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.visitor.AstVisitor

fun AstNode.toSerializable(nodeTypeSet: NodeTypeSet): Structure {
    return Structure(
            nodeTypeSet = nodeTypeSet,
            ast = this.accept(SerializableAstBuildVisitor(nodeTypeSet.toNodeTypePool()))
    )
}

fun AstNode.toSerializable(nodeTypePool: NodeTypePool): Structure {
    return Structure(
            nodeTypeSet = nodeTypePool.toSerializable(),
            ast = this.accept(SerializableAstBuildVisitor(nodeTypePool))
    )
}

private class SerializableAstBuildVisitor(
        private val types: NodeTypePool
) : AstVisitor<SerializableAst.Node> {
    override fun visit(node: AstNode): SerializableAst.Node {
        return SerializableAst.TerminalNode(   // TODO
                type = -1,
                text = ""
        )
    }

    override fun visitRule(node: AstRuleNode): SerializableAst.Node {
        val type = types.getRuleType(node.ruleName) ?: throw NoSuchElementException()
        return if (node is NormalAstRuleNode) {
            SerializableAst.NormalizedRuleNode(
                    type.index,
                    text = node.getText()
            )
        } else {
            SerializableAst.RuleNode(
                    type.index,
                    children = node.children?.map { it.accept(this) }.orEmpty()
            )
        }
    }

    override fun visitTerminal(node: AstTerminalNode): SerializableAst.Node {
        val type = types.getTokenType(node.tokenType) ?: throw NoSuchElementException()
        return SerializableAst.TerminalNode(
                type = type.index,
                text = node.token
        )
    }
}