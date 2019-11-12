package io.github.durun.nitron.core.ast.basic

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class BasicAstRuleNode
private constructor(
        @JsonProperty("ruleName")
        override val ruleName: String,

        @JsonProperty("children")
        override val children: List<AstNode>,

        @JsonProperty("range")
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

    @JsonIgnore
    override fun getText(): String {
        return children.joinToString(" ") { it.getText() }
    }
}