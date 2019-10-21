package io.github.durun.nitron.data.model.table

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

abstract class ReadableTable<V>(name: String) : Table(name) {
    abstract fun read(row: ResultRow): V
}
