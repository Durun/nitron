package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.NodeType

class RuleType internal constructor(
        override val index: Int,
        override val name: String
) : NodeType, Map.Entry<Int, String> {
    override val key: Int
        get() = index
    override val value: String
        get() = name
}