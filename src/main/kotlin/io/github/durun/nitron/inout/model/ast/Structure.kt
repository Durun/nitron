package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.codeHashOf

/**
 * コード片の構文木情報.
 * エクスポート可能
 */
class Structure internal constructor(
        /**
         * [asts]の文法が持つtokenType, ruleNameの集合
         */
        val nodeTypePool: NodeTypePool,

        /**
         * 構文木
         */
        val asts: List<AstNode>,

        /**
         * コード片のMD5ハッシュ
         */
        val hash: ByteArray = codeHashOf(asts.joinToString(" ") { it.getText() })
) {
    constructor(nodeTypePool: NodeTypePool, ast: AstNode, hash: ByteArray = codeHashOf(ast.getText())) : this(nodeTypePool, listOf(ast))

    fun merge(others: List<Structure>): Structure {
        return Structure(
                nodeTypePool = this.nodeTypePool,
                asts = this.asts + others.flatMap { it.asts }
        )
    }

    override fun toString(): String {
        return "Structure(${nodeTypePool.grammar}: $asts)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Structure

        if (nodeTypePool != other.nodeTypePool) return false
        if (asts != other.asts) return false
        if (!hash.contentEquals(other.hash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeTypePool.hashCode()
        result = 31 * result + asts.hashCode()
        result = 31 * result + hash.contentHashCode()
        return result
    }
}


fun merge(structures: Iterable<Structure>): Structure? {
    val first = structures.firstOrNull()
    val remain = structures.drop(1)
    return first?.merge(remain)
}