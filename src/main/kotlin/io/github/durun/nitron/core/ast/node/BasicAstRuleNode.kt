package io.github.durun.nitron.core.ast.node

class BasicAstRuleNode(
        override val type: Rule,
        children: List<AstNode>
) : AstRuleNode {
    @Deprecated("may cause hash conflict", ReplaceWith("this(type = , children = children)"))
    constructor(ruleName: String, children: List<AstNode>) : this(
            type = Rule(ruleName.hashCode(), ruleName),
            children = children
    )

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