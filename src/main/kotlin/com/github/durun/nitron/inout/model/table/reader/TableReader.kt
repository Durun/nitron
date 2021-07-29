package com.github.durun.nitron.inout.model.table.reader

import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Table

interface TableReader<V> {
    fun read(statement: Table.() -> Query): Sequence<V>
    fun read(): Sequence<V>
}