package io.github.durun.nitron.core.ast.node


interface NodeType : Map.Entry<Int, String> {
    val index: Int
        get() = this.key
    val name: String
        get() = this.value
}