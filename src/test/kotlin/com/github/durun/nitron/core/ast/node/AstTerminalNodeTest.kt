package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.AstSerializers
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.ast.type.TokenType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class AstTerminalNodeTest : FreeSpec({
	"property" - {
		val type = TokenType(1, "Type")
        val node = AstTerminalNode.of("text", type, 2)
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
		val format = AstSerializers.json(types)

		"serialize" {
            val node = AstTerminalNode.of("text", types.getTokenType(1)!!, 3)

			val json = format.encodeToString(node)
			println(json)

			val node2: AstTerminalNode = format.decodeFromString(json)
			node2 shouldNotBeSameInstanceAs node
			node2 shouldBe node
			node2.type shouldBeSameInstanceAs node.type
		}
	}
})
