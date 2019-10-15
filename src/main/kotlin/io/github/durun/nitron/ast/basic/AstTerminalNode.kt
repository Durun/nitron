package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.basic.TextRange
import io.github.durun.nitron.ast.AstVisitor

class AstTerminalNode(
        @JsonProperty("token")
        val token: String,

        @JsonProperty("tokenType")
        val tokenType: String,

        @JsonProperty("children")
        override val children: List<AstNode>? = null,

        @JsonProperty("range")
        override val range: TextRange
) : AstNode {
        override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitTerminal(this)

        @JsonIgnore
        override fun getText(): String = token

        override fun contains(range: TextRange): Boolean
                = this.range.contains(range)

        override fun pickByRules(rules: Collection<String>): List<AstNode> = emptyList()
        override fun pickRecursiveByRules(rules: Collection<String>): List<AstNode> = pickByRules(rules)
        override fun mapChildren(map: (AstNode) -> AstNode): AstTerminalNode = this
}