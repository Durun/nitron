package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty

class AstTerminalNode(
        @JsonProperty("token")
        val token: String,

        @JsonProperty("children")
        override val children: List<AstNode>? = null,
) : AstNode {

}