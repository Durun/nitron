package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.ast.AstNode

class BasicAstRuleNode(
        @JsonProperty("ruleName")
        val ruleName: String,

        @JsonProperty("children")
        override val children: List<AstNode>
): AstNode {
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
}
