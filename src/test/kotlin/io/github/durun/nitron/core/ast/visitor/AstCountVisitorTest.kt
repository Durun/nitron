package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec


class AstCountVisitorTest : StringSpec({
	"returns size of basic tree" {
		nodeOf("root",
				nodeOf("TOKEN", "A"),
				nodeOf("TOKEN", "B")
		)
				.accept(AstCountVisitor)
				.size shouldBe 3
	}
	"returns size one" {
		nodeOf("root", "A")
				.accept(AstCountVisitor)
				.size shouldBe 1
	}
	"returns size of nested tree" {
		nodeOf("root",
				nodeOf("TOKEN", "A"),
				nodeOf("statement",
						nodeOf("expression",
							nodeOf("ID", "x"),
							nodeOf("OPERATOR", "+="),
							nodeOf("LITERAL", "1")
						),
						nodeOf("TOKEN", "S")
				)
		)
				.accept(AstCountVisitor)
				.size shouldBe 8
	}
})

fun nodeOf(type: String, token: String) = AstTerminalNode(token, type, line = 0)
fun nodeOf(type: String, vararg children: AstNode) = BasicAstRuleNode(ruleName = type, children = listOf(*children))