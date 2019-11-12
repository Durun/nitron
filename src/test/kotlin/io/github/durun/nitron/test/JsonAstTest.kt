package io.github.durun.nitron.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.inout.model.ast.Node
import io.github.durun.nitron.inout.model.ast.NormalizedRuleNode
import io.github.durun.nitron.inout.model.ast.RuleNode
import io.github.durun.nitron.inout.model.ast.TerminalNode
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec

class JsonAstTest : FreeSpec() {
    private val mapper = jacksonObjectMapper()

    private fun ruleNodeOf(rule: Int, vararg children: Node) = RuleNode(rule, children.toList())

    init {
        "a" {
            val node = ruleNodeOf(1,
                    TerminalNode(5, "term1"),
                    ruleNodeOf(2,
                            TerminalNode(4, "term2"),
                            TerminalNode(5, "term3"),
                            NormalizedRuleNode(2, "abst")
                    ),
                    NormalizedRuleNode(2, "abst"),
                    TerminalNode(4, "terminal")
            )

            val json = mapper.writeValueAsString(node)
            println(json)
            val node2 = mapper.readValue<Node>(json)
            val text = mapper.writeValueAsString(node2)
            json shouldBe text
        }
        "b" {
            val node = TerminalNode(5, "term")
            val json = mapper.writeValueAsString(node)
            println(json)
            val node2 = mapper.readValue<Node>(json)
            val text = mapper.writeValueAsString(node2)
            json shouldBe text
        }
    }
}