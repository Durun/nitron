package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.ast.type.NodeType
import com.github.durun.nitron.core.ast.visitor.AstVisitor

/**
 * 構文木
 */
interface AstNode {

    val type: NodeType

    /**
     * 子ノード
     */
    val children: List<AstNode>?

    /**
     * このノードがコピーである場合, コピー元のノード.
     * このノードがオリジナルである場合, このノード自身.
     * このノードがコピーのコピーである場合, おおもとのコピー元のノード.
     */
    val originalNode: AstNode

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