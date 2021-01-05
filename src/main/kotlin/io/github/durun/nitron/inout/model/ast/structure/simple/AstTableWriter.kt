package io.github.durun.nitron.inout.model.ast.structure.simple

import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.ast.table.NodeTypePools
import io.github.durun.nitron.inout.model.ast.table.Structures
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path

const val defaultBufferSize = 10000

private fun <T> Sequence<T>.toListSequence(size: Int): Sequence<List<T>> = this.windowed(size = size, step = size, partialWindows = true)

class AstTableWriter(
        private val db: Database,
        private val bufferSize: Int = defaultBufferSize
) {
    constructor(path: Path, bufferSize: Int = defaultBufferSize) : this(
            db = SQLiteDatabase.connect(path),
            bufferSize = bufferSize
    )

    init {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(NodeTypePools, Structures)
        }
    }

    fun writeBuffering(typeSet: NodeTypePool, nodeSequence: Sequence<Pair<ByteArray, String>>) {
        writeBuffering(
                typeSetId = writeIfNotExist(typeSet),
                nodeSequence = nodeSequence
        )
    }

    private fun writeBuffering(typeSetId: Int, nodeSequence: Sequence<Pair<ByteArray, String>>) {
        val buffers = nodeSequence.toListSequence(size = bufferSize)
        buffers.forEach { buffer ->
            transaction(db) {
                Structures.batchInsertRawValues(typeSetId, buffer)
            }
        }
    }

    private fun writeIfNotExist(typeSet: NodeTypePool): Int {
        return transaction {
            NodeTypePools
                    .select { NodeTypePools.grammar eq typeSet.grammar }
                    .map { it[NodeTypePools.id] }
                    .firstOrNull()
                    ?: kotlin.runCatching {
                        transaction { NodeTypePools.insert(typeSet) }
                        NodeTypePools
                                .select { NodeTypePools.grammar eq typeSet.grammar }
                                .map { it[NodeTypePools.id] }
                                .first()
                    }.getOrThrow()
        }
    }
}