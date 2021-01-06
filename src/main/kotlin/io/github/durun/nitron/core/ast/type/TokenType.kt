package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.NodeType
import kotlinx.serialization.Serializable

@Serializable(with = DefaultTokenTypeSerializer::class)
class TokenType(
        override val index: Int,
        override val name: String
) : NodeType, Map.Entry<Int, String> {
    override val key: Int
        get() = index
    override val value: String
        get() = name
}