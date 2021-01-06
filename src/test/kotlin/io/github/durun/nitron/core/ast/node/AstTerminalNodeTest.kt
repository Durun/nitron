package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.type.TokenType
import io.github.durun.nitron.core.ast.type.TokenTypeSerializer
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

class AstTerminalNodeTest : FreeSpec({
	"property" - {
		val type = TokenType(1, "Type")
		val node = AstTerminalNode("text", type, 2)
		"children" {
			node.children shouldBe null
		}
		"getText" {
			node.getText() shouldBe "text"
		}
		"type" {
			node.type shouldBeSameInstanceAs type
		}
		"line" {
			node.line shouldBe 2
		}
	}

	"serialize" - {
		val types = NodeTypePool.of("grammar",
				tokenTypes = listOf(TokenType(1, "Type")),
				ruleTypes = listOf()
		)
		val format = Json {
			serializersModule = SerializersModule {
				contextual(TokenTypeSerializer(types))
			}
		}
		
		"serialize" {
			val node = AstTerminalNode("text", types.getTokenType(1)!!, 3)

			val json = format.encodeToString(node)
			println(json)

			val node2: AstTerminalNode = format.decodeFromString(json)
			node2 shouldNotBeSameInstanceAs node
			node2 shouldBe node
			node2.type shouldBeSameInstanceAs node.type
		}
	}
})
