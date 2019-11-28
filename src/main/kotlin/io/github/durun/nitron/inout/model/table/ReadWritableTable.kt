package io.github.durun.nitron.inout.model.table

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement

abstract class ReadWritableTable<V>(name: String) : ReadableTable<V>(name) {
    abstract fun insert(value: V, insertId: Int? = null): InsertStatement<Number>

    fun getNextId(idColumn: Column<Int>): Int {
        val all = this.slice(idColumn).selectAll()
        val dec = all.orderBy(idColumn, SortOrder.DESC)
        val max = dec.firstOrNull()?.get(idColumn) ?: 0
        return max + 1
    }
}