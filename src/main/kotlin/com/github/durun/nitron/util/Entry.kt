package com.github.durun.nitron.util

internal fun <K, V> entryOf(key: K, value: V): Map.Entry<K, V> = SimpleEntry(key, value)

internal class SimpleEntry<K, V> constructor(
        override val key: K,
        override val value: V
) : Map.Entry<K, V> {
	override fun toString(): String = "{$key: $value}"
}