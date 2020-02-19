package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.core.ast.visitor.AstVisitor

fun AstNode.toSerializable(nodeTypeSet: NodeTypeSet): Structure {
    return Structure(
            nodeTypeSet = nodeTypeSet,
            ast = this.accept(SerializableAstBuildVisitor(nodeTypeSet))
    )
}

private class SerializableAstBuildVisitor(
        private val typeMap: NodeTypeSet
) : AstVisitor<SerializableAst.Node> {
    override fun visit(node: AstNode): SerializableAst.Node {
        return SerializableAst.TerminalNode(   // TODO
                type = -1,
                text = ""
        )
    }

    override fun visitRule(node: AstRuleNode): SerializableAst.Node {
        val type = typeMap.rule(node.type.name) ?: throw NoSuchElementException()
        return if (node is NormalAstRuleNode) {
            SerializableAst.NormalizedRuleNode(
                    type,
                    text = node.getText()
            )
        } else {
            SerializableAst.RuleNode(
                    type,
                    children = node.children?.map { it.accept(this) }.orEmpty()
            )
        }
    }

    override fun visitTerminal(node: AstTerminalNode): SerializableAst.Node {
        return SerializableAst.TerminalNode(
                type = typeMap.token(node.type.name) ?: throw NoSuchElementException(),
                text = node.token
        )
    }
}