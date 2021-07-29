package com.github.durun.nitron.core.ast.type


interface NodeType : Map.Entry<Int, String> {
    val index: Int
        get() = this.key
    val name: String
        get() = this.value

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}