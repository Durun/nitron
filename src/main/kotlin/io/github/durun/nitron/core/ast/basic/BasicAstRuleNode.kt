package io.github.durun.nitron.core.ast.basic

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.core.ast.normalizing.normalizeByRules

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
    override fun getText(): String? {
        return children
                .mapNotNull { it.getText() }
                .joinToString(" ")
    }

    override fun pickByRules(rules: Collection<String>): List<AstNode> {
        return if (rules.contains(this.ruleName))
            listOf(this)
        else
            children.flatMap { it.pickByRules(rules) }
    }

    override fun pickRecursiveByRules(rules: Collection<String>): List<AstNode> {
        return this.pickByRules(rules)
                .flatMap { node ->
                    val subtrees = node
                            .children
                            ?.flatMap { it.pickRecursiveByRules(rules) }
                            ?: emptyList()
                    val normNode = node
                            .mapChildren { it.normalizeByRules(rules) }
                    listOf(listOf(normNode), subtrees).flatten()
                }
    }

    override fun mapChildren(map: (AstNode) -> AstNode): BasicAstRuleNode {
        val newChildren = this.children.map(map)
        return BasicAstRuleNode(ruleName, newChildren)
    }
}