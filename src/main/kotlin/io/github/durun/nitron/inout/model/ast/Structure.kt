package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.codeHashOf

/**
 * コード片の構文木情報.
 * エクスポート可能
 */
class Structure internal constructor(
        /**
         * [ast]の文法が持つtokenType, ruleNameの集合
         */
        val nodeTypeSet: NodeTypeSet,

        /**
         * 構文木
         */
        val ast: SerializableAst.Node,

        /**
         * コード片のMD5ハッシュ
         */
        val hash: ByteArray
) {
    constructor(nodeTypeSet: NodeTypeSet, ast: SerializableAst.Node) : this(
            nodeTypeSet = nodeTypeSet,
            ast = ast,
            hash = codeHashOf(ast.text)
    )
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Structure

        if (nodeTypeSet != other.nodeTypeSet) return false
        if (ast != other.ast) return false
        if (!hash.contentEquals(other.hash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeTypeSet.hashCode()
        result = 31 * result + ast.hashCode()
        result = 31 * result + hash.contentHashCode()
        return result
    }
}