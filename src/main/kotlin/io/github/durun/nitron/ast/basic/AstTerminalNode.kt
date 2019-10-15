package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.ast.AstNode

class AstTerminalNode(
        @JsonProperty("token")
        val token: String,

        @JsonProperty("children")
        override val children: List<AstNode>? = null,
) : AstNode {

}