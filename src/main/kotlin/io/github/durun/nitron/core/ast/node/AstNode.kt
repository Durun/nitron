package io.github.durun.nitron.core.ast.node

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.durun.nitron.core.ast.visitor.AstVisitor

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(
        JsonSubTypes.Type(name = "Rule", value = BasicAstRuleNode::class),
        JsonSubTypes.Type(name = "Terminal", value = AstTerminalNode::class),
        JsonSubTypes.Type(name = "NormalizedRule", value = NormalAstRuleNode::class)
)
/**
 * 構文木
 */
interface AstNode {
    /**
     * 元のソースコードとの対応位置
     */
    val range: TextRange?

    /**
     * 子ノード
     */
    val children: List<AstNode>?

    fun <R> accept(visitor: AstVisitor<R>): R = visitor.visit(this)

    /**
     * 元のソースコードを返す.
     * ただし空白は再現されない.
     * @return 元のソースコード
     */
    fun getText(): String
}