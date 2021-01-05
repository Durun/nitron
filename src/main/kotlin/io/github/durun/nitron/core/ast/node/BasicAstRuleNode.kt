package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.type.RuleType

class BasicAstRuleNode(
        override val type: RuleType,
        children: List<AstNode>
) : AstRuleNode {
    override var children: List<AstNode> = children
        private set

    override fun getText(): String {
        return children.joinToString(" ") { it.getText() }
    }

    override fun replaceChildren(newChildren: List<AstNode>): AstRuleNode {
        this.children = newChildren
        return this
    }

    override fun copyWithChildren(children: List<AstNode>): AstRuleNode {
        return BasicAstRuleNode(type, children)
    }
}