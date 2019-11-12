package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.core.ast.visitor.AstVisitor

class JsonAstBuildVisitor(
        private val typeMap: NodeTypeMap
) : AstVisitor<Node> {
    override fun visit(node: AstNode): Node {
        return TerminalNode(   // TODO
                type = -1,
                text = ""
        )
    }

    override fun visitRule(node: AstRuleNode): Node {
        val type = typeMap.rule(node.ruleName) ?: throw NoSuchElementException()
        return if (node is NormalAstRuleNode) {
            NormalizedRuleNode(
                    type,
                    text = node.getText()
            )
        } else {
            RuleNode(
                    type,
                    children = node.children?.map { it.accept(this) }.orEmpty()
            )
        }
    }

    override fun visitTerminal(node: AstTerminalNode): Node {
        return TerminalNode(
                type = typeMap.token(node.tokenType) ?: throw NoSuchElementException(),
                text = node.token
        )
    }
}