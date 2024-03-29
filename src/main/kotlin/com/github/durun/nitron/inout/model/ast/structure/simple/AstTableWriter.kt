package com.github.durun.nitron.inout.model.ast.structure.simple

import com.github.durun.nitron.core.MD5
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.inout.model.ast.table.NodeTypePools
import com.github.durun.nitron.inout.model.ast.table.Structures
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

const val defaultBufferSize = 10000

private fun <T> Sequence<T>.toListSequence(size: Int): Sequence<List<T>> = this.windowed(size = size, step = size, partialWindows = true)

class AstTableWriter(
        private val db: Database,
        private val bufferSize: Int = defaultBufferSize
) {
    init {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(NodeTypePools, Structures)
        }
    }

    fun writeBuffering(typeSet: NodeTypePool, nodeSequence: Sequence<Pair<MD5, String>>) {
        writeBuffering(
                typeSetId = writeIfNotExist(typeSet),
                nodeSequence = nodeSequence
        )
    }

    private fun writeBuffering(typeSetId: Int, nodeSequence: Sequence<Pair<MD5, String>>) {
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
                ?: run {
                    transaction { NodeTypePools.insert(typeSet) }
                    NodeTypePools
                        .select { NodeTypePools.grammar eq typeSet.grammar }
                        .map { it[NodeTypePools.id] }
                        .first()
                }
        }
    }
}