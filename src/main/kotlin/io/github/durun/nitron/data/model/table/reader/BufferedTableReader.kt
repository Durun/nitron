package io.github.durun.nitron.data.model.table.reader

import io.github.durun.nitron.data.model.table.ReadableTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

internal class BufferedTableReader<V>(
        private val db: Database,
        private val table: ReadableTable<V>,
        private val bufferSize: Int = 10000,
        private val transform: (ResultRow) -> V = { table.read(it) }
): TableReader<V> {
    override fun read(statement: Table.() -> Query): Sequence<V> = sequence {
        var i = 0
        do {
            val sequence = transaction(db) {
                table.statement()
                        .limit(n = bufferSize, offset = i)
                        .map(transform)
            }.asSequence()
            i += bufferSize
            yieldAll(sequence)
        } while (sequence.iterator().hasNext())
    }

    override fun read(): Sequence<V> = read { selectAll() }
}