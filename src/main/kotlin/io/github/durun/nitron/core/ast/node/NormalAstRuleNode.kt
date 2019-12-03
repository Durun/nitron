package io.github.durun.nitron.core.ast.node

/**
 * 部分木の情報を除くことで抽象化された非終端ノード.
 *
 * @param [originalNode] 元の非終端ノード
 */
class NormalAstRuleNode(
        private val originalNode: AstRuleNode,
        private val text: String? = null
) : AstRuleNode {
    override val ruleName: String = originalNode.ruleName
    override val children: List<AstNode>? = null
    override val range: TextRange? = originalNode.range
    override fun getText(): String = text ?: ruleName.toUpperCase()
}

