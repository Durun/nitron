package io.github.durun.nitron.inout.model.cpanalyzer.table

import org.jetbrains.exposed.sql.statements.UpdateBuilder

abstract class ReadWritableTable<V>(name: String) : ReadableTable<V>(name) {
    abstract fun write(value: V, insertId: Long? = null): UpdateBuilder<Number>
}