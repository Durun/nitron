package io.github.durun.nitron.core.ast.node

/**
 * 部分木の情報を除くことで抽象化された非終端ノード.
 */
class NormalAstRuleNode(
        override val type: Rule,
        private val text: String? = null
) : AstRuleNode {
    /**
     *  @param [originalNode] 元の非終端ノード
     */
    constructor(originalNode: AstRuleNode, text: String? = null) : this(
            type = originalNode.type,
            text = text
    )

    override val children: List<AstNode>?
        get() = null

    override fun getText(): String = text ?: type.name.toUpperCase()

    override fun replaceChildren(newChildren: List<AstNode>): AstRuleNode {
        return this
    }
}

