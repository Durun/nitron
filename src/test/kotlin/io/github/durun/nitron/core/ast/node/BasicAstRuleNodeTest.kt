package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.AstSerializers
import io.github.durun.nitron.core.ast.type.RuleType
import io.github.durun.nitron.core.ast.type.TokenType
import io.github.durun.nitron.core.ast.type.createNodeTypePool
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class BasicAstRuleNodeTest : FreeSpec({
	fun nodeOf(type: RuleType, vararg children: AstNode) = BasicAstRuleNode(type, children.toMutableList())
	fun nodeOf(type: TokenType, text: String, line: Int) = AstTerminalNode(text, type, line)
	fun nodeOf(type: RuleType, text: String) = NormalAstRuleNode(type, text)

	"property" - {
		val rule = RuleType(1, "Rule")
		val token = TokenType(2, "Token")
		val children = arrayOf(
			nodeOf(token, "text1", 0),
			nodeOf(
				rule,
				nodeOf(token, "text2", 1),
						nodeOf(token, "text3", 2),
						nodeOf(rule,
								nodeOf(token, "text4", 3)
						),
						nodeOf(rule, "normText")
				),
				nodeOf(rule, "V"),
				nodeOf(token, "end", 9)
		)
		val tree = nodeOf(rule, *children)

		"getText" {
			tree.getText() shouldBe "text1 text2 text3 text4 normText V end"
		}
		"type" {
			tree.type shouldBeSameInstanceAs rule
		}
		"children" {
			tree.children shouldContainExactly children.toList()
		}
	}

	"operation" - {
		// TODO
		"replaceChildren" { }
		"copyWithChildren" { }
	}

	"serialize" - {
		val types = createNodeTypePool("grammar",
				tokenTypes = listOf("Token"),
				ruleTypes = listOf("Rule")
		)
		val format = AstSerializers.json(types)

		"serialize tree" {
			val rule = types.getRuleType("Rule")!!
			val token = types.getTokenType("Token")!!
			val tree = nodeOf(rule,
					nodeOf(token, "text1", 0),
					nodeOf(rule,
							nodeOf(token, "text2", 1),
							nodeOf(token, "text3", 2),
							nodeOf(rule,
									nodeOf(token, "text4", 3)
							),
							nodeOf(rule, "normText")
					),
					nodeOf(rule, "V"),
					nodeOf(token, "end", 9)
			)

			val json = format.encodeToString(tree)
			println(json)

			val tree2: BasicAstRuleNode = format.decodeFromString(json)
			tree2 shouldNotBeSameInstanceAs tree
			tree2 shouldBe tree
			tree2.type shouldBeSameInstanceAs tree.type
		}
	}
})
