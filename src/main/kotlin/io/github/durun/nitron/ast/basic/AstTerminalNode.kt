package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.basic.TextRange

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
        override fun getText(): String = token

}