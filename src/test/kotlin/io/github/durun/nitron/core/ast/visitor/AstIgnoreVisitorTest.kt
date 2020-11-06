package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.NitronException
import io.github.durun.nitron.core.ast.node.NodeTypePool
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FreeSpec

class AstIgnoreVisitorTest : FreeSpec({
	"Instantiation"- {
		"Should throw if given rule is wrong" {
			val types = NodeTypePool(
					tokenTypes = listOf("TOKEN_A", "TOKEN_B"),
					ruleNames = listOf("rule_a", "rule_b")
			)
			val ignoreTypes = listOf("TOKEN_B", "strange_rule")

			shouldThrow<NitronException> {
				astIgnoreVisitorOf(types, ignoreTypes)
			}
		}
	}
})