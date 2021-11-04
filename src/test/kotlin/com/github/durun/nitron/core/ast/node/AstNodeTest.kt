package com.github.durun.nitron.core.ast.node

import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import com.github.durun.nitron.testutil.astNode
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class AstNodeTest : FreeSpec({
    val token = TokenType(0, "TOKEN")
    val rule = RuleType(1, "rule")
    "trace original node" - {
        "copy of terminal node" {
            val node = astNode("token1", token, 1)
            val copied = node.copy()
            copied shouldBe node
            copied shouldNotBeSameInstanceAs node
            copied.originalNode shouldBeSameInstanceAs node

            val doubleCopied = copied.copy()
            doubleCopied shouldNotBeSameInstanceAs copied
            doubleCopied.originalNode shouldBeSameInstanceAs node
        }

        "copy of rule node" {
            val node = astNode(rule) {
                token("token1", token)
                node(rule) {
                    token("token2", token)
                }
            }
            val copied = node.copy()
            copied shouldBe node
            copied shouldNotBeSameInstanceAs node
            copied.originalNode shouldBeSameInstanceAs node

            copied.children.first().originalNode shouldBeSameInstanceAs node.children.first()
            copied.children.last().originalNode shouldBeSameInstanceAs node.children.last()
            copied.children.last().children!!.first().originalNode shouldBeSameInstanceAs node.children.last().children!!.first()

            val doubleCopied = copied.copy()
            doubleCopied shouldNotBeSameInstanceAs copied
            doubleCopied.originalNode shouldBeSameInstanceAs node
        }
    }

    "traceable parent node" - {
        "simple case" {
            var node1: AstNode? = null
            var node2: AstNode? = null
            val tree = astNode(rule) {
                token("token1", token)
                node2 = node(rule) {
                    node1 = token("token2", token)
                }
            }
            node1?.parent shouldBeSameInstanceAs node2
            node2?.parent shouldBeSameInstanceAs tree
        }
        "replaced children" {
            var node1: AstNode? = null
            val tree = astNode(rule) {
                node1 = token("token1", token)
            }
            val node2 = astNode(rule) {
                token("token2", token)
            }
            tree.parent shouldBe null
            node1?.parent shouldBeSameInstanceAs tree
            tree.clearChildren()
            node1?.parent shouldBe null
            tree.addChild(node2)
            node2.parent shouldBeSameInstanceAs tree
            tree.removeChildAt(0)
            node2.parent shouldBe null
            tree.addChildren(listOf(node1!!, node2))
            node1?.parent shouldBeSameInstanceAs tree
            node2.parent shouldBeSameInstanceAs tree
            tree.removeChildIf { true }
            node1?.parent shouldBe null
            node2.parent shouldBe null
            tree.addChild(node1!!)
            tree.setChild(0, node2)
            node1?.parent shouldBe null
            node2.parent shouldBeSameInstanceAs tree
        }
        "prohibit reuse of child node" {
            shouldThrowAny {
                val child = astNode("token", token, 1)
                BasicAstRuleNode.of(rule, listOf(child, child))
            }

            var node1: AstNode? = null
            val tree = astNode(rule) {
                node1 = token("token1", token)
            }
            val child = node1!!
            shouldThrowAny { tree.addChild(child) }
            shouldThrowAny { tree.addChildren(listOf(child)) }
            shouldThrowAny { tree.setChild(0, child) }
        }
    }
})

