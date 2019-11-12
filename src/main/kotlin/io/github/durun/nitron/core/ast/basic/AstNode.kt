package io.github.durun.nitron.core.ast.basic

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.durun.nitron.core.ast.normalizing.NormalAstRuleNode
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
    fun getText(): String?

    /**
     * [AstNode]を分割する.
     * レシーバーの部分木(自身を含む)のうち,
     * ルートが[rules]のいずれかに該当する全ての最大の木を返す.
     * @param [rules] 非終端規則
     * @return 分割後の[AstNode]のリスト
     */
    fun pickByRules(rules: Collection<String>): List<AstNode>

    /**
     * [AstNode]を分割する.
     * レシーバーの部分木のうち, ルートが[rules]のいずれかに該当する
     * (1. 全ての最小の木),
     * (2. 全ての最小でない木)
     * を返す.
     * (2)の真部分木のうち, ルートが[rules]のいずれかに該当する木は[NormalAstRuleNode]に置き換える.
     * @param [rules] 非終端規則
     * @return 分割後の[AstNode]のリスト
     */
    fun pickRecursiveByRules(rules: Collection<String>): List<AstNode>

    /**
     * 子ノードを[map]により置き換える.
     */
    fun mapChildren(map: (AstNode) -> AstNode): AstNode
}