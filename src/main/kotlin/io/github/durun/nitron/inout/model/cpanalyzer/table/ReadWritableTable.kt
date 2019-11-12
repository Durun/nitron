package io.github.durun.nitron.inout.model.cpanalyzer.table

import org.jetbrains.exposed.sql.statements.InsertStatement

abstract class ReadWritableTable<V>(name: String) : ReadableTable<V>(name) {
    abstract fun insert(value: V, insertId: Int? = null): InsertStatement<Number>
}