package io.github.durun.nitron.ast

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.durun.nitron.ast.basic.AstTerminalNode

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(
        JsonSubTypes.Type(name = "Rule", value = AstRuleNode::class),
        JsonSubTypes.Type(name = "Terminal", value = AstTerminalNode::class)
)
interface AstNode {
    val children: List<AstNode>?
}