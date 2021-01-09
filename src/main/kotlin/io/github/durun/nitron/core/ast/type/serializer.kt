package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.inout.model.ast.Structure
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
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
		contextual(TokenTypeSerializer(types))
		contextual(RuleTypeSerializer(types))
		contextual(StructureSerializer(types))
		polymorphic(AstNode::class) {
			subclass(AstTerminalNode::class)
			subclass(NormalAstRuleNode::class)
			subclass(BasicAstRuleNode::class)
		}
	}

	val encodeOnlyJson: Json = json(emptyTypes)
}

private val emptyTypes = NodeTypePool.of("", emptySet())

object DefaultTokenTypeSerializer : KSerializer<TokenType> by TokenTypeSerializer(emptyTypes)
object DefaultRuleTypeSerializer : KSerializer<RuleType> by RuleTypeSerializer(emptyTypes)

object NodeTypePoolSerializer : KSerializer<NodeTypePool> {
	private val dummySerializer = serializer<Dummy>()

	override val descriptor: SerialDescriptor = dummySerializer.descriptor

	override fun serialize(encoder: Encoder, value: NodeTypePool) {
		val data = Dummy(
				grammar = value.grammar,
				tokenType = value.mainTokenTypes.associate { it.index to it.name },
				ruleType = value.ruleTypes.associate { it.index to it.name },
				synonymTokenType = (value.tokenTypes - value.mainTokenTypes)
						.associate { it.name to it.index }
		)
		dummySerializer.serialize(encoder, data)
	}

	override fun deserialize(decoder: Decoder): NodeTypePool {
		val data = dummySerializer.deserialize(decoder)
		return NodeTypePool.of(
				grammarName = data.grammar,
				tokenTypes = data.tokenType,
				ruleTypes = data.ruleType,
				synonymTokenTypes = data.synonymTokenType
		)
	}

	@Serializable
	private class Dummy(
			val grammar: String,
			val tokenType: Map<Int, String>,
			val ruleType: Map<Int, String>,
			val synonymTokenType: Map<String, Int>
	)
}


class TokenTypeSerializer(
		private val types: NodeTypePool
): KSerializer<TokenType> {
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

class RuleTypeSerializer(
		private val types: NodeTypePool
): KSerializer<RuleType> {
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
			@Serializable(with = HashSerializer::class)
			val hash: ByteArray
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