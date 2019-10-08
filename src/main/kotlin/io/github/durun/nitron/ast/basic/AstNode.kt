package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

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
}