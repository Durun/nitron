package io.github.durun.nitron.core.ast.type

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

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