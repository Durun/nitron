package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.codeHashOf

/**
 * コード片の構文木情報.
 * エクスポート可能
 */
class Structure internal constructor(
        /**
         * [ast]の文法が持つtokenType, ruleNameの集合
         */
        val nodeTypePool: NodeTypePool,

        /**
         * 構文木
         */
        val ast: SerializableAst.Node,

        /**
         * コード片のMD5ハッシュ
         */
        val hash: ByteArray
) {
    constructor(nodeTypePool: NodeTypePool, ast: SerializableAst.Node) : this(
            nodeTypePool = nodeTypePool,
            ast = ast,
            hash = codeHashOf(ast.text)
    )

    fun merge(others: List<Structure>): Structure {
        val nodes = others.map { it.ast }
                .toMutableList()
        nodes.add(0, this.ast)
        val newAst = SerializableAst.NodeList(nodes)
        return Structure(
                nodeTypePool = nodeTypePool,
                ast = newAst
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Structure

        if (nodeTypePool != other.nodeTypePool) return false
        if (ast != other.ast) return false
        if (!hash.contentEquals(other.hash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeTypePool.hashCode()
        result = 31 * result + ast.hashCode()
        result = 31 * result + hash.contentHashCode()
        return result
    }
}


fun merge(structures: Iterable<Structure>): Structure? {
    val first = structures.firstOrNull()
    val remain = structures.drop(1)
    return first?.merge(remain)
}