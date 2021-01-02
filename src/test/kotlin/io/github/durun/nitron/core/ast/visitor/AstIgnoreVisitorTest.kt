package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.NitronException
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class AstIgnoreVisitorTest : FreeSpec({
	"Instantiation"- {
		"created if given rule is ok" {
			val types = NodeTypePool(
					tokenTypes = listOf("TOKEN_A", "TOKEN_B"),
					ruleNames = listOf("rule_a", "rule_b")
			)
			val ignoreTypes = listOf("rule_a")

			astIgnoreVisitorOf(types, ignoreTypes).shouldBeInstanceOf<AstIgnoreVisitor>()
		}
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