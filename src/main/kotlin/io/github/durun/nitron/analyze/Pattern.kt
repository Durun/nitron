package io.github.durun.nitron.analyze

import io.github.durun.nitron.inout.model.ast.SerializableAst

data class Pattern(
        val node: Pair<SerializableAst.Node, SerializableAst.Node>,
        val hash: Pair<ByteArray, ByteArray>
) {
    constructor(node: Pair<SerializableAst.Node, SerializableAst.Node>) : this(
            node,
            hash = node.first.toHash() to node.second.toHash()
    )

    constructor(
            beforeNode: SerializableAst.Node,
            afterNode: SerializableAst.Node,
            beforeHash: ByteArray,
            afterHash: ByteArray
    ) : this(
            node = beforeNode to afterNode,
            hash = beforeHash to afterHash
    )
}
