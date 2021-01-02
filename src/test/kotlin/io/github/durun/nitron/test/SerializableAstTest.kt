package io.github.durun.nitron.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.inout.model.ast.SerializableAst
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SerializableAstTest : FreeSpec() {
    private val mapper = jacksonObjectMapper()

    private fun ruleNodeOf(rule: Int, vararg children: SerializableAst.Node) = SerializableAst.RuleNode(rule, children.toList())

    init {
        "SerializableAst is writable and readable" {
            val node = ruleNodeOf(1,
                    SerializableAst.TerminalNode(5, "term1"),
                    ruleNodeOf(2,
                            SerializableAst.TerminalNode(4, "term2"),
                            SerializableAst.TerminalNode(5, "term3"),
                            SerializableAst.NormalizedRuleNode(2, "abst")
                    ),
                    SerializableAst.NormalizedRuleNode(2, "abst"),
                    SerializableAst.TerminalNode(4, "terminal")
            )

            val json = mapper.writeValueAsString(node)
            println(json)
            val node2: SerializableAst.Node = mapper.readValue(json)
            val text = mapper.writeValueAsString(node2)
            json shouldBe text
        }
    }
}