package com.github.durun.nitron.core.ast.path

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import com.github.durun.nitron.core.ast.type.NodeType
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import org.jaxen.dom.DOMXPath
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

abstract class AstPath {
	companion object {
		private fun fromXPath(expression: String): AstPath {
			return AstXPath(expression)
		}

		private fun of(type: NodeType): AstPath {
            return SimpleAstPath(type)
        }

        @JvmStatic
        fun of(expression: String, types: NodeTypePool): AstPath {
            val typeName = if (expression.startsWith('/')) expression.drop(1) else expression
            return types.getType(typeName)?.let { of(it) }
                ?: fromXPath(expression)
        }

		fun of(expression: String): AstPath {
			return fromXPath(expression)
		}
	}

    abstract fun select(ast: AstNode): List<AstNode>
    abstract fun selectPath(ast: AstNode): List<List<Int>>

	/**
	 * [root]を根とする構文木から、XPathの指すノードを[replacement]に従って置換します。
	 * 置換対象が根の場合は置換後の根を返します。
	 * このメソッドは[root]に変更を与えます。
	 */
	abstract fun replaceNode(root: AstNode, replacement: (AstNode) -> AstNode): AstNode

	/**
	 * [root]を根とする構文木から、XPathの指すノードを削除します。
	 * 削除対象が根の場合はnullを返します。
	 * このメソッドは[root]に変更を与えます。
	 */
	abstract fun removeNode(root: AstNode): AstNode?
}

private class AstXPath(expression: String) : AstPath() {
	companion object {
        private val xmlBuilder = ThreadLocal.withInitial { DocumentBuilderFactory.newInstance().newDocumentBuilder() }
        private fun AstNode.toXml(): Node {
            val str = this.accept(AstXmlBuildVisitor)
            return xmlBuilder.get().parse(str.byteInputStream())
        }
    }

	private val xpath = DOMXPath(expression)

    override fun select(ast: AstNode): List<AstNode> {
        val nodes: List<Node> = xpath.selectNodes(ast.toXml()).filterIsInstance<Node>()
        return nodes.mapNotNull {
            val path = it.getIndexPath().drop(1)
            ast.resolve(path)
        }
    }

    override fun selectPath(ast: AstNode): List<List<Int>> {
        val nodes: List<Node> = xpath.selectNodes(ast.toXml()).filterIsInstance<Node>()
        return nodes.map { it.getIndexPath().drop(1) }
    }

	override fun replaceNode(root: AstNode, replacement: (AstNode) -> AstNode): AstNode {
		val nodes: List<Node> = xpath.selectNodes(root.toXml()).filterIsInstance<Node>()
		nodes.forEach {
			val path = it.getIndexPath().drop(1)
            if (path.isEmpty()) return replacement(root)
            val childIndex = path.last()
            when (val parent = root.resolve(path.dropLast(1))) {
                is BasicAstRuleNode -> parent.setChild(childIndex, replacement(parent.children[childIndex]))
            }
		}
		return root
	}

	override fun removeNode(root: AstNode): AstNode? {
		selectWithParent(root).sortedByDescending { (_, n) -> n }.forEach { (parent, childIndex) ->
            if (parent == null) return null
            else parent.removeChildAt(childIndex)
        }
		return root
	}

	private fun selectWithParent(root: AstNode): List<Pair<BasicAstRuleNode?, Int>> {
		val nodes: List<Node> = xpath.selectNodes(root.toXml()).filterIsInstance<Node>()
		return nodes.map {
			val path = it.getIndexPath().drop(1)
			if (path.isEmpty()) return@map null to 0
			val parent = root.resolve(path.dropLast(1)) as BasicAstRuleNode
			val childIndex = path.last()
			parent to childIndex
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
        val selects: MutableList<AstNode> = mutableListOf()
        val queue = ArrayDeque<AstNode>()
        queue.add(ast)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            current.children?.let { queue.addAll(it) }
            if (current.type == type) selects.add(current)
        }
        return selects
    }

    override fun selectPath(ast: AstNode): List<List<Int>> {
        TODO()
    }

	override fun replaceNode(root: AstNode, replacement: (AstNode) -> AstNode): AstNode {
		if (root.type == type) return replacement(root)
		selectWithParent(root).forEach { (parent, childIndex) ->
			check(parent is BasicAstRuleNode)
            parent.setChild(childIndex, replacement(parent.children[childIndex]))
		}
		return root
	}

	override fun removeNode(root: AstNode): AstNode? {
		if (root.type == type) return null
		selectWithParent(root).sortedByDescending { (_, n) -> n }.forEach { (parent, childIndex) ->
			check(parent is BasicAstRuleNode)
            parent.removeChildAt(childIndex)
		}
		return root
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