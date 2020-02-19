package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.core.ast.node.NodeTypePool
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class NodeTypePoolReader(private val db: Database) {
    fun readById(id: Int): NodeTypePool {
        return readSingle { NodeTypeSets.id eq id }
    }

    private fun readSingle(where: SqlExpressionBuilder.() -> Op<Boolean>): NodeTypePool {
        return read(where).first()
                .toNodeTypePool()
    }

    private fun read(where: SqlExpressionBuilder.() -> Op<Boolean>): List<NodeTypeSet> {
        return transaction(db) {
            NodeTypeSets
                    .select(where)
                    .map { NodeTypeSets.read(it) }
        }
    }
}