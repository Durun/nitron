package io.github.durun.nitron.inout.model.cpanalyzer.table

import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

abstract class ReadableTable<V>(name: String) : Table(name) {
    abstract fun read(row: ResultRow, alias: Alias<ReadableTable<V>>? = null): V
}
