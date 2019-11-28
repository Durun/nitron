package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.reader.BufferedTableReader
import io.github.durun.nitron.inout.model.table.reader.TableReader
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class StructuresReader(
        private val db: Database
) : TableReader<Structure> {
    private val nodeTypeSetsReader: TableReader<NodeTypeSet> = BufferedTableReader(db, NodeTypeSets)

    override fun read(statement: Table.() -> Query): Sequence<Structure> {
        val nodeTypeSets = nodeTypeSetsReader.read()

        return transaction(db) {
            nodeTypeSets
                    .mapIndexed { index, it -> index to it }.flatMap { (index, nodeTypeSet) ->
                        val nodeTypeSetId = index + 1
                        transaction {
                            Structures
                                    .statement()
                                    .andWhere { Structures.nodeTypeSet eq nodeTypeSetId }
                                    .map {
                                        Structures.read(it, nodeTypeSet)
                                    }
                        }.asSequence()
                    }
        }
    }

    override fun read(): Sequence<Structure> = read { selectAll() }
}