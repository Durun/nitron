package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty

class AstRuleNode(
        @JsonProperty("ruleName")
        val ruleName: String,

        @JsonProperty("children")
        override val children: List<AstNode>
): AstNode {
}