package io.github.durun.nitron.core.ast.visitor

import io.github.durun.nitron.core.NitronException
import io.github.durun.nitron.core.ast.type.createNodeTypePool
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class AstIgnoreVisitorTest : FreeSpec({
	"Instantiation"- {
		"created if given rule is ok" {
			val types = createNodeTypePool(
					grammarName = "lang",
					tokenTypes = listOf("TOKEN_A", "TOKEN_B"),
					ruleTypes = listOf("rule_a", "rule_b")
			)
			val ignoreTypes = listOf("rule_a")

			astIgnoreVisitorOf(types, ignoreTypes).shouldBeInstanceOf<AstIgnoreVisitor>()
		}
		"Should throw if given rule is wrong" {
			val types = createNodeTypePool(
					grammarName = "lang",
					tokenTypes = listOf("TOKEN_A", "TOKEN_B"),
					ruleTypes = listOf("rule_a", "rule_b")
			)
			val ignoreTypes = listOf("TOKEN_B", "strange_rule")

			shouldThrow<NitronException> {
				astIgnoreVisitorOf(types, ignoreTypes)
			}
		}
	}
})