package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.digest
import io.github.durun.nitron.core.ast.type.NodeTypePool
import kotlinx.serialization.Serializable

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
        val hash: MD5 = MD5.digest(asts)
) {
    constructor(nodeTypePool: NodeTypePool, ast: AstNode, hash: MD5 = MD5.digest(ast)) : this(nodeTypePool, listOf(ast), hash)

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
        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeTypePool.hashCode()
        result = 31 * result + asts.hashCode()
        result = 31 * result + hash.hashCode()
        return result
    }
}


fun merge(structures: Iterable<Structure>): Structure? {
    val first = structures.firstOrNull()
    val remain = structures.drop(1)
    return first?.merge(remain)
}