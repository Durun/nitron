package io.github.durun.nitron.inout.model.table.writer

interface TableWriter<V> {
    fun write(value: V) = write(sequenceOf(value))
    fun write(values: List<V>) = write(values.asSequence())
    fun write(values: Sequence<V>)
}