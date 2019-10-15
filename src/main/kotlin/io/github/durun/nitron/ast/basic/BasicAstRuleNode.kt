package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.ast.AstNode

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

    override fun getText(): String?
            = children.mapNotNull { it.getText() }
            .joinToString(" ")

    override fun contains(range: TextRange): Boolean
            = this.range?.contains(range) ?: false

    override fun pickByRules(rules: Collection<String>): List<AstNode>
            = if (rules.contains(this.ruleName))
        listOf(this)
    else    children.flatMap { it.pickByRules(rules) }

}
