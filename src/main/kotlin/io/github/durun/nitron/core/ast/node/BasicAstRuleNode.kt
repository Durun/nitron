package io.github.durun.nitron.core.ast.node

class BasicAstRuleNode
constructor(
        override val ruleName: String,
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
}