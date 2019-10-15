package io.github.durun.nitron.ast

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.durun.nitron.ast.basic.BasicAstRuleNode
import io.github.durun.nitron.ast.basic.AstTerminalNode
import io.github.durun.nitron.ast.basic.TextRange

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(
        JsonSubTypes.Type(name = "Rule", value = BasicAstRuleNode::class),
        JsonSubTypes.Type(name = "Terminal", value = AstTerminalNode::class)
)
interface AstNode {
    val range: TextRange?
    val children: List<AstNode>?

    fun <R> accept(visitor: AstVisitor<R>) = visitor.visit(this)
    fun getText(): String?
}