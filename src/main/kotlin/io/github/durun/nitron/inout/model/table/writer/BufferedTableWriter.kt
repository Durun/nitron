package io.github.durun.nitron.inout.model.table.writer

import io.github.durun.nitron.inout.model.table.ReadWritableTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.transactions.transaction

internal class BufferedTableWriter<V>(
        private val table: ReadWritableTable<V>,
        private val idColumn: Column<Int>,
        private val bufferSize: Int = 100
) : TableWriter<V> {
    override fun write(values: List<V>) {
        transaction {
            val nextId = getNextId()
            values.forEachIndexed { index, v ->
                table.insert(v, insertId = nextId + index)
            }
        }
    }

    override fun write(values: Sequence<V>) {
        values.windowed(size = bufferSize, step = bufferSize, partialWindows = true) {
            write(it)
        }
    }

    private fun getNextId(): Int = table.getNextId(idColumn)
}