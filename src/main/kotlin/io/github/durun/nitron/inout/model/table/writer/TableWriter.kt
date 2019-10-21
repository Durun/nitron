package io.github.durun.nitron.inout.model.table.writer

interface TableWriter<V> {
    fun write(value: V)
    fun write(values: List<V>)
    fun write(values: Sequence<V>)
}