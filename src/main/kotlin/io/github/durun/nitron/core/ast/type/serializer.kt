package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.inout.model.ast.Structure
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer

object AstSerializers {
	fun json(types: NodeTypePool): Json = Json {
		serializersModule = module(types)
		classDiscriminator = "T"
	}

	private fun module(types: NodeTypePool) = SerializersModule {
		contextual(TokenType.Serializer(types))
		contextual(RuleType.Serializer(types))
		contextual(StructureSerializer(types))
		polymorphic(AstNode::class) {
			subclass(AstTerminalNode::class)
			subclass(NormalAstRuleNode::class)
			subclass(BasicAstRuleNode::class)
		}
	}

	val encodeOnlyJson: Json = json(NodeTypePool.EMPTY)
}


class StructureSerializer(
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

object HashSerializer: KSerializer<ByteArray> {
	override val descriptor: SerialDescriptor= PrimitiveSerialDescriptor("ByteArrayAsHex", PrimitiveKind.STRING)
	override fun serialize(encoder: Encoder, value: ByteArray) {
		encoder.encodeString(value.joinToString("") { String.format("%02x", it) })
	}

	override fun deserialize(decoder: Decoder): ByteArray {
		return decoder.decodeString()
				.chunked(2)
				.map { Integer.decode("0x$it").toByte() }
				.toByteArray()
	}
}