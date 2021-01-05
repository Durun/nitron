package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.reader.BufferedTableReader
import io.github.durun.nitron.inout.model.table.reader.TableReader
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class StructuresReader(
        private val db: Database
) : TableReader<Structure> {
    private val tableReader: TableReader<NodeTypePool> = BufferedTableReader(db, NodeTypePools)

    override fun read(statement: Table.() -> Query): Sequence<Structure> {
        val nodeTypePools = tableReader.read()

        return transaction(db) {
            nodeTypePools
                    .mapIndexed { index, it -> index to it }.flatMap { (index, nodeTypePool) ->
                        val nodeTypePoolId = index + 1
                        transaction {
                            Structures
                                    .statement()
                                    .andWhere { Structures.nodeTypeSet eq nodeTypePoolId }
                                    .map {
                                        Structures.read(it, nodeTypePool)
                                    }
                        }.asSequence()
                    }
        }
    }

    override fun read(): Sequence<Structure> = read { selectAll() }
}