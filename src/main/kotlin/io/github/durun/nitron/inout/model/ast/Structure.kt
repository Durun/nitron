package io.github.durun.nitron.inout.model.ast

import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.digest
import io.github.durun.nitron.core.ast.type.NodeTypePool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

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

    class Serializer(
            private val types: NodeTypePool
    ) : KSerializer<Structure> {
        override val descriptor = PrimitiveSerialDescriptor("Structure", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Structure) {
            val data = Dummy(asts = value.asts, hash = value.hash)
            encoder.encodeSerializableValue(dummySerializer, data)
        }

        override fun deserialize(decoder: Decoder): Structure {
            val data = decoder.decodeSerializableValue(dummySerializer)
            return Structure(nodeTypePool = types, asts = data.asts, hash = data.hash)
        }

        companion object {
            private val dummySerializer = serializer<Dummy>()
        }

        @Serializable
        private class Dummy(
                val asts: List<AstNode>,
                val hash: MD5
        )
    }
}


fun merge(structures: Iterable<Structure>): Structure? {
    val first = structures.firstOrNull()
    val remain = structures.drop(1)
    return first?.merge(remain)
}