package io.github.durun.nitron.util

inline fun <reified K, reified V> Map<*, *>.filterIsInstance(): Map<K, V> {
	val newMap = mutableMapOf<K, V>()
	this.entries
			.filterIsInstance<Map.Entry<K, V>>()
			.forEach { (key, value) ->
				newMap[key] = value
			}
	return newMap
}

fun <V> Map<Int, V>.toSparseList(): List<V?> {
	val maxIndex = this.keys.maxOrNull() ?: return emptyList()
	return (0..maxIndex).map { this[it] }
}