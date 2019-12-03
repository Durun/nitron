package io.github.durun.nitron.core.ast.node

class BasicAstRuleNode
private constructor(
        override val ruleName: String,
        override val children: List<AstNode>,
        override val range: TextRange?
) : AstRuleNode {
    constructor(
            ruleName: String,
            children: List<AstNode>
    ) : this(
            ruleName = ruleName,
            children = children,
            range = children
                    .mapNotNull { it.range }
                    .let { validRange ->
                        if (validRange.isEmpty()) {
                            null
                        } else {
                            val first = validRange.first()
                            val last = validRange.last()
                            first.include(last)
                        }
                    }
    )

    override fun getText(): String {
        return children.joinToString(" ") { it.getText() }
    }
}