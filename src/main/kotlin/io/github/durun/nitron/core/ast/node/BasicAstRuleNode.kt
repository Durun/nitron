package io.github.durun.nitron.core.ast.node

class BasicAstRuleNode
constructor(
        override val ruleName: String,
        override val children: List<AstNode>
) : AstRuleNode {
    override fun getText(): String {
        return children.joinToString(" ") { it.getText() }
    }
}