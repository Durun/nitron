package io.github.durun.nitron.inout.model.ast


class NodeTypeMap(
        private val tokenTypeMap: ArrayMap<String>,
        private val ruleNameMap: ArrayMap<String>
) {
    fun token(index: Int): String? = tokenTypeMap.byIndex(index)
    fun token(type: String): Int? = tokenTypeMap[type]
    fun rule(index: Int): String? = ruleNameMap.byIndex(index)
    fun rule(name: String): Int? = ruleNameMap[name]
}

class ArrayMap<T>(
        private val array: Array<T>
) : Map<T, Int> by array.associateBy({ it }, { array.indexOf(it) }) {
    fun byIndex(index: Int): T? = array[index]
}