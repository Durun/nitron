package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.NodeType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RuleType.Serializer.Default::class)
class RuleType constructor(
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

        other as RuleType

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
    ) : KSerializer<RuleType> {
        object Default : KSerializer<RuleType> by Serializer(NodeTypePool.EMPTY)

        override val descriptor = PrimitiveSerialDescriptor("RuleType", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: RuleType) {
            encoder.encodeInt(value.index)
        }

        override fun deserialize(decoder: Decoder): RuleType {
            val index = decoder.decodeInt()
            return types.getRuleType(index)
                    ?: throw IllegalStateException("failed to deserialize: type $index")
        }
    }
}