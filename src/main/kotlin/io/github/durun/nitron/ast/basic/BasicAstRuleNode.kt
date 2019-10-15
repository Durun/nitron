package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.normalizing.normalizeByRules

class BasicAstRuleNode(
        @JsonProperty("ruleName")
        override val ruleName: String,

        @JsonProperty("children")
        override val children: List<AstNode>

): AstRuleNode {
        @JsonProperty("range")
        override val range: TextRange?
        init {
                val validRange = this.children.mapNotNull { it.range }
                range = if (validRange.isEmpty()) {
                        null
                } else {
                        val first = validRange.first()
                        val last = validRange.last()
                        TextRange(first.start, last.stop)
                }
        }

    @JsonIgnore
    override fun getText(): String?
            = children.mapNotNull { it.getText() }
            .joinToString(" ")

    override fun contains(range: TextRange): Boolean
            = this.range?.contains(range) ?: false

    override fun pickByRules(rules: Collection<String>): List<AstNode>
            = if (rules.contains(this.ruleName))
        listOf(this)
    else    children.flatMap { it.pickByRules(rules) }

    override fun pickRecursiveByRules(rules: Collection<String>): List<AstNode>
            = this.pickByRules(rules).flatMap {node ->
        val subtrees = node.children?.flatMap { it.pickRecursiveByRules(rules) } ?: emptyList()
        val normNode = node.mapChildren { it.normalizeByRules(rules) }
        listOf(listOf(normNode), subtrees).flatten()
    }

    override fun mapChildren(map: (AstNode) -> AstNode): BasicAstRuleNode {
        val newChildren = this.children.map(map)
        return BasicAstRuleNode(ruleName, newChildren)
    }
}