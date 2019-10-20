package io.github.durun.nitron.data.model.table.reader

import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Table

interface TableReader<V> {
    fun toSequence(statement: Table.() -> Query): Sequence<V>
    fun toSequence(): Sequence<V>
}