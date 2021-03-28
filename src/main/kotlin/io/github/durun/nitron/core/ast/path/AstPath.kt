package io.github.durun.nitron.core.ast.path

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.type.NodeType
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import org.jaxen.dom.DOMXPath
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

abstract class AstPath {
	companion object {
		private fun fromXPath(expression: String): AstPath {
			return AstXPath(expression)
		}

		private fun fromOneType(type: NodeType): AstPath {
			return SimpleAstPath(type)
		}

		fun of(expression: String, types: NodeTypePool): AstPath {
			return types.getType(expression.trim('/'))?.let { fromOneType(it) }
				?: fromXPath(expression)
		}

		fun of(expression: String): AstPath {
			return fromXPath(expression)
		}
	}

	abstract fun select(ast: AstNode): List<AstNode>
	abstract fun replaceNode(root: AstNode, replacement: (AstNode) -> AstNode)
}

private class AstXPath(expression: String) : AstPath() {
	companion object {
		private val xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		private fun AstNode.toXml(): Node {
			val str = this.accept(AstXmlBuildVisitor)
			return xmlBuilder.parse(str.byteInputStream())
		}
	}

	private val xpath = DOMXPath(expression)

	override fun select(ast: AstNode): List<AstNode> {
		val nodes: List<Node> = xpath.selectNodes(ast.toXml()).filterIsInstance<Node>()
		return nodes.mapNotNull {
			val indices = it.getIndexPath()
			ast.resolve(indices)
		}
	}

	override fun replaceNode(root: AstNode, replacement: (AstNode) -> AstNode) {
		val nodes: List<Node> = xpath.selectNodes(root.toXml()).filterIsInstance<Node>()
		nodes.forEach {
			val path = it.getIndexPath().drop(1)
			val parent = root.resolve(path.dropLast(1)) as BasicAstRuleNode
			val childIndex = path.last()
			parent.children[childIndex] = replacement(parent.children[childIndex])
		}
	}

	private fun NodeList.toList(): List<Node> = (0 until this.length).map { item(it) }
	private fun Node.getIndexPath(): List<Int> {
		val indices: MutableList<Int> = mutableListOf()
		var node = this
		var parent = parentNode
		while (parent != null) {
			indices.add(parent.childNodes.toList().indexOf(node))
			node = parent
			parent = parent.parentNode
		}
		return indices.apply { reverse() }
	}
}

private class SimpleAstPath(
	private val type: NodeType
) : AstPath() {
	override fun select(ast: AstNode): List<AstNode> {
		TODO("Not yet implemented")
	}

	override fun replaceNode(root: AstNode, replacement: (AstNode) -> AstNode) {
		selectWithParent(root).forEach { (parent, childIndex) ->
			if (parent is BasicAstRuleNode) parent.children[childIndex] = replacement(parent.children[childIndex])
		}
	}

	private fun selectWithParent(ast: AstNode): List<Pair<AstNode, Int>> {
		val selects: MutableList<Pair<AstNode, Int>> = mutableListOf()
		val queue = ArrayDeque<AstNode>()
		queue.add(ast)
		while (queue.isNotEmpty()) {
			val current = queue.removeFirst()
			current.children?.let { queue.addAll(it) }
			current.children?.forEachIndexed { i, child ->
				if (child.type == type) selects.add(current to i)
			}
		}
		return selects
	}
}

/**
 * [path]で与えられた順番に子要素へアクセスし、最終的に選択された要素を返します。
 * 例えば listOf(1,2,3) が与えられれば、node.children[1].children[2].children[3] を返します。
 * @param path 子要素のインデックスのリスト
 */
private fun AstNode.resolve(path: List<Int>): AstNode? {
	if (path.isEmpty()) return this
	var target: AstNode? = this
	path.forEach {
		target = target?.children?.get(it)
	}
	return target
}