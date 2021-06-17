package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.type.NodeType
import io.github.durun.nitron.core.ast.visitor.AstVisitor

/**
 * 構文木
 */
interface AstNode {

    val type: NodeType

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

    /**
     * deep copy
     */
    fun copy(): AstNode
}