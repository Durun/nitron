package com.github.durun.nitron.core.ast.type

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TokenType.Serializer.Default::class)
class TokenType(
        override val index: Int,
        override val name: String
) : NodeType, Map.Entry<Int, String> {
    override val key: Int
        get() = index
    override val value: String
        get() = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TokenType

        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + name.hashCode()
        return result
    }

    class Serializer(
            private val types: NodeTypePool
    ) : KSerializer<TokenType> {
        object Default : KSerializer<TokenType> by Serializer(NodeTypePool.EMPTY)

        override val descriptor = PrimitiveSerialDescriptor("TokenType", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: TokenType) {
            encoder.encodeInt(value.index)
        }

        override fun deserialize(decoder: Decoder): TokenType {
            val index = decoder.decodeInt()
            return types.getTokenType(index)
                    ?: throw IllegalStateException("failed to deserialize: type $index")
        }
    }
}