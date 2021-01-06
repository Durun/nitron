package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.ast.type.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

class NormalAstRuleNodeTest : FreeSpec({
	"prooperty" - {
		val type = RuleType(1, "Rule")
		val node = NormalAstRuleNode(type, "Text")
		"children" {
			node.children shouldBe null
		}
		"getText" {
			node.getText() shouldBe "Text"
		}
		"type" {
			node.type shouldBeSameInstanceAs type
		}
	}

	"serialize" - {
		val types = NodeTypePool.of("grammar",
				tokenTypes = listOf(TokenType(1, "Token")),
				ruleTypes = listOf(RuleType(2, "Rule"))
		)
		val format = Json {
			serializersModule = SerializersModule {
				contextual(RuleTypeSerializer(types))
			}
		}

		"serialize" {
			val node = NormalAstRuleNode(types.getRuleType(2)!!, "text")

			val json = format.encodeToString(node)
			println(json)

			val node2: NormalAstRuleNode = format.decodeFromString(json)
			node2 shouldNotBeSameInstanceAs node
			node2 shouldBe node
			node2.type shouldBeSameInstanceAs node.type
		}
	}
})
