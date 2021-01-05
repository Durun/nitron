package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.type.TokenType
import io.github.durun.nitron.core.ast.type.TokenTypeSerializer
import io.github.durun.nitron.core.ast.type.createNodeTypePool
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

class AstTerminalNodeTest : FreeSpec({
	val node = AstTerminalNode("text", TokenType(1, "Type"), 2)

	"children" {
		node.children shouldBe null
	}

	"getText" {
		node.getText() shouldBe "text"
	}

	"type" {
		node.type.name shouldBe "Type"
		node.type.index shouldBe 1
	}

	"line" {
		node.line shouldBe 2
	}

	"serialize" {
		val types = NodeTypePool.of("grammar",
				tokenTypes = listOf(TokenType(1, "Type")),
				ruleTypes = listOf()
		)
		val node1 = AstTerminalNode("text", types.getTokenType(1)!!, 3)

		val format = Json {
			serializersModule = SerializersModule {
				contextual(TokenTypeSerializer(types))
			}
		}

		val json = format.encodeToString(node1)

		val node2: AstTerminalNode = format.decodeFromString(json)
		node2 shouldBe node1
		node2.type shouldBeSameInstanceAs node1.type
	}
})
