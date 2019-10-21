package io.github.durun.nitron.data.model.table.writer

import io.github.durun.nitron.data.model.table.ReadWritableTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

internal class BufferedTableWriter<V>(
        private val db: Database,
        private val table: ReadWritableTable<V>,
        private val idColumn: Column<Int>,
        private val bufferSize: Int = 100
) : TableWriter<V> {
    override fun write(value: V) {
        transaction(db) {
            val nextId = getNextId(table)
            table.insert(value, insertId = nextId)
        }
    }

    override fun write(values: List<V>) {
        transaction(db) {
            val nextId = getNextId(table)
            values.forEachIndexed { index, v ->
                table.insert(v, insertId = nextId + index)
            }
        }
    }

    override fun write(values: Sequence<V>) {
        val buffer = values.take(bufferSize).toList()
        write(buffer)
    }

    private fun getNextId(table: Table): Int {
        val all = table.slice(idColumn).selectAll()
        val dec = all.orderBy(idColumn, SortOrder.DESC)
        val max = dec.firstOrNull()?.get(idColumn) ?: 0
        return max + 1
    }
}