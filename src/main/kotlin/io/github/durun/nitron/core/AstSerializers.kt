package io.github.durun.nitron.core

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.type.RuleType
import io.github.durun.nitron.core.ast.type.TokenType
import io.github.durun.nitron.inout.model.ast.Structure
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object AstSerializers {
	fun json(types: NodeTypePool): Json = Json {
		serializersModule = module(types)
		classDiscriminator = "T"
	}

	private fun module(types: NodeTypePool) = SerializersModule {
		contextual(TokenType.Serializer(types))
		contextual(RuleType.Serializer(types))
		contextual(Structure.Serializer(types))
		polymorphic(AstNode::class) {
			subclass(AstTerminalNode::class)
			subclass(NormalAstRuleNode::class)
			subclass(BasicAstRuleNode::class)
		}
	}

	val encodeOnlyJson: Json = json(NodeTypePool.EMPTY)
}
