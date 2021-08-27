package com.github.durun.nitron.core.parser.srcml

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import com.github.durun.nitron.core.ast.type.NodeType
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import com.github.durun.nitron.core.parser.NitronParser
import com.github.durun.nitron.core.parser.NitronParsers
import com.github.durun.nitron.core.parser.jdt.AlignLineVisitor
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.Reader
import javax.xml.parsers.DocumentBuilderFactory

fun init(
    command: String,
    language: String,
    nodeTypes: NodeTypePool,
    nodeTypeMapping: Map<String, Map<Map<String, String>, NodeType>>
): NitronParser = NitronParsers.srcml(command, language, nodeTypes, nodeTypeMapping)

@Suppress("UNUSED")
fun NitronParsers.srcml(
    command: String,
    language: String,
    nodeTypes: NodeTypePool,
    nodeTypeMapping: Map<String, Map<Map<String, String>, NodeType>>
): NitronParser = SrcmlParser(command, language, nodeTypes, nodeTypeMapping)

private class SrcmlParser(
    private val srcmlCommand: String = "srcml",
    val language: String,
    nodeTypes: NodeTypePool,
    private val nodeTypeMapping: Map<String, Map<Map<String, String>, NodeType>>
) : NitronParser {
    private val TOKEN = TokenType(-1, "TOKEN")
    override val nodeTypes: NodeTypePool = NodeTypePool.of(
        nodeTypes.grammar,
        nodeTypes.tokenTypes + TOKEN,
        nodeTypes.ruleTypes
    )

    override fun parse(reader: Reader): AstNode {
        val process = ProcessBuilder(srcmlCommand, "--language", language, "--position")
            .start()
        process.outputStream.bufferedWriter().use { writer ->
            writer.write(reader.readText())
        }

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.parse(process.inputStream.buffered())

        return toAstNode(document.documentElement)!!
            .accept(AlignLineVisitor())
    }


    private data class Context(val line: Int)

    private fun Node.getPosLine(): Int? {
        return this.attributes?.getNamedItem("pos:line")?.nodeValue?.toInt()
    }

    private fun Node.getAstNodeType(): NodeType? {
        return nodeTypeMapping[this.nodeName]?.entries?.firstOrNull { (attr, _) ->
            attr.entries.all { (k, v) -> this.attributes.getNamedItem(k)?.nodeValue == v }
        }?.let { (_, type) -> type }
            ?: nodeTypes.getType(this.nodeName)
    }

    private fun toAstNode(node: Node, ctx: Context? = null): AstNode? {

        return when (node.nodeType) {
            Node.TEXT_NODE -> {
                val text = node.nodeValue.trim()
                    .takeIf { it.isNotBlank() } ?: return null  // ignore blank text element
                // get line from tags like <pos:position pos:line="2" pos:column="7"/>
                val line = node.nextSibling?.takeIf { it.nodeName == "pos:position" }?.getPosLine()
                    ?: ctx?.line
                    ?: 0
                AstTerminalNode(text, TOKEN, line)
            }
            Node.ELEMENT_NODE -> {
                when (val type = node.getAstNodeType()) {
                    is TokenType -> {
                        val text = node.firstChild?.nodeValue?.trim()
                            ?: throw Exception("type $type may not be terminal node")
                        val line = node.getPosLine()
                            ?: ctx?.line
                            ?: 0
                        AstTerminalNode(text, type, line)
                    }
                    is RuleType -> {
                        val line = node.getPosLine()
                        val ctx = if (line != null) Context(line) else ctx
                        val children = node.childNodes.asSequence().mapNotNull { toAstNode(it, ctx) }.toMutableList()
                        if (children.isNotEmpty()) BasicAstRuleNode(type, children)
                        else null
                    }
                    null -> null
                    else -> throw Exception("AST NodeType is unknown: $type")
                }
            }
            else -> throw Exception("XML node type is unknown: type=${node.nodeType}")
        }
    }

    private fun NodeList.asSequence(): Sequence<Node> {
        val list = this
        return sequence {
            for (i in 0 until list.length) {
                yield(list.item(i))
            }
        }
    }
}