package io.github.durun.nitron.core.ast.normalizing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.core.ast.AstNode
import io.github.durun.nitron.core.ast.basic.AstRuleNode
import io.github.durun.nitron.core.ast.basic.TextRange

/**
 * 部分木の情報を除くことで抽象化された非終端ノード.
 *
 * @param [originalNode] 元の非終端ノード
 */
class NormalAstRuleNode(
        private val originalNode: AstRuleNode,
        private val text: String? = null
) : AstRuleNode {
    @JsonProperty("ruleName")
    override val ruleName: String = originalNode.ruleName

    @JsonProperty("children")
    override val children: List<AstNode>? = null

    @JsonProperty("range")
    override val range: TextRange? = originalNode.range

    @JsonIgnore
    override fun getText(): String = text ?: ruleName.toUpperCase()

    override fun pickByRules(rules: Collection<String>): List<AstNode> {
        return if (rules.contains(this.ruleName))
            listOf(this)
        else
            emptyList()
    }

    override fun pickRecursiveByRules(rules: Collection<String>): List<AstNode> = pickByRules(rules)

    override fun mapChildren(map: (AstNode) -> AstNode): NormalAstRuleNode = this
}

