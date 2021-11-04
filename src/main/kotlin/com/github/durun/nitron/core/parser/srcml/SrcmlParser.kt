package com.github.durun.nitron.core.parser.srcml

import com.github.durun.nitron.core.ParsingException
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
import org.xml.sax.SAXException
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
        val document = try {
            val process = ProcessBuilder(srcmlCommand, "--language", language, "--position")
                .start()
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(reader.readText())
            }
            process.onExit().thenAccept {
                val exitValue = it.exitValue()
                if (exitValue != 0) throw ParsingException("srcML terminated with exitValue=$exitValue")
            }
            val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            builder.parse(process.inputStream.buffered())
        } catch (e: SAXException) {
            throw ParsingException("Failed to parse XML from srcML: ${e.message}", e)
        } catch (e: Exception) {
            throw ParsingException("Internal error: ${e.message}", e)
        }

        val ast: AstNode = try {
            toAstNode(document.documentElement)!!
        } catch (e: Exception) {
            throw ParsingException("Failed to convert XML into nitron tree: ${e.message}", e)
        }
        return ast.accept(AlignLineVisitor())
    }

    private data class Position(val start: Int, val end: Int)

    private fun Node.getPosition(): Position? {
        // ex.) <tag pos:start="1:1" pos:end="1:13"></tag>
        val attr = this.attributes ?: return null
        val startItem = attr.getNamedItem("pos:start") ?: return null
        val endItem = attr.getNamedItem("pos:end") ?: return null
        return Position(
            start = startItem.nodeValue.split(':').first().toInt(),
            end = endItem.nodeValue.split(':').first().toInt()
        )
    }

    private fun Node.getAstNodeType(): NodeType? {
        return nodeTypeMapping[this.nodeName]?.entries?.firstOrNull { (attr, _) ->
            attr.entries.all { (k, v) -> this.attributes.getNamedItem(k)?.nodeValue == v }
        }?.let { (_, type) -> type }
            ?: nodeTypes.getType(this.nodeName)
    }

    private fun toAstNode(node: Node, parentPos: Position? = null): AstNode? {
        return when (node.nodeType) {
            Node.TEXT_NODE -> {
                val text = node.nodeValue.trim()
                    .takeIf { it.isNotBlank() } ?: return null  // ignore blank text element
                val line = run {
                    val next = node.nextSibling ?: return@run parentPos?.end
                    val prev = node.previousSibling ?: return@run parentPos?.start
                    prev.getPosition()?.end
                        ?: next.getPosition()?.start
                } ?: 0
                AstTerminalNode(text, TOKEN, line)
            }
            Node.ELEMENT_NODE -> {
                when (val type = node.getAstNodeType()) {
                    is TokenType -> {
                        val text = node.firstChild?.nodeValue?.trim()
                            ?: throw ParsingException("type $type may not be terminal node")
                        val line = node.getPosition()?.start
                            ?: 0
                        AstTerminalNode(text, type, line)
                    }
                    is RuleType -> {
                        val pos = node.getPosition()
                        val children = node.childNodes.asSequence().mapNotNull { toAstNode(it, pos) }.toMutableList()
                        if (children.isNotEmpty()) BasicAstRuleNode.of(type, children)
                        else null
                    }
                    null -> null
                    else -> throw ParsingException("AST NodeType is unknown: $type")
                }
            }
            else -> throw ParsingException("XML node type is unknown: type=${node.nodeType}")
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