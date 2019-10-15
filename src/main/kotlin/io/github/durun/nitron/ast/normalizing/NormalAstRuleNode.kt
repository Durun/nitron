package io.github.durun.nitron.ast.normalizing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.basic.BasicAstRuleNode
import io.github.durun.nitron.ast.basic.AstRuleNode
import io.github.durun.nitron.ast.basic.TextRange

class NormalAstRuleNode(
        private val originalNode: BasicAstRuleNode
): AstRuleNode {
    @JsonProperty("ruleName")
    override val ruleName: String = originalNode.ruleName

    @JsonProperty("children")
    override val children: List<AstNode>? = null

    @JsonProperty("range")
    override val range: TextRange? = originalNode.range

    override fun contains(range: TextRange): Boolean = originalNode.contains(range)

    @JsonIgnore
    override fun getText(): String = ruleName.toUpperCase()

    override fun pickByRules(rules: Collection<String>): List<AstNode>
        = if (rules.contains(this.ruleName))
                listOf(this)
        else    emptyList()

    override fun pickRecursiveByRules(rules: Collection<String>): List<AstNode> = pickByRules(rules)

    override fun mapChildren(map: (AstNode) -> AstNode): NormalAstRuleNode = this
}

