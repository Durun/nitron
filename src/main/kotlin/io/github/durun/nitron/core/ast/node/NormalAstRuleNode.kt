package io.github.durun.nitron.core.ast.node

/**
 * 部分木の情報を除くことで抽象化された非終端ノード.
 */
class NormalAstRuleNode(
        override val ruleName: String,
        override val range: TextRange?,
        private val text: String? = null
) : AstRuleNode {
    /**
     *  @param [originalNode] 元の非終端ノード
     */
    constructor(originalNode: AstRuleNode, text: String? = null): this(
            ruleName = originalNode.ruleName,
            range = originalNode.range,
            text = text
    )

    override val children: List<AstNode>?
        get() = null

    override fun getText(): String = text ?: ruleName.toUpperCase()
}

