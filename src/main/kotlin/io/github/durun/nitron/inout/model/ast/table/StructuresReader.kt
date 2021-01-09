package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.reader.BufferedTableReader
import io.github.durun.nitron.inout.model.table.reader.TableReader
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object StructuresReader : TableReader<Structure> {
    private val tableReader: TableReader<NodeTypePool> = BufferedTableReader(NodeTypePools)

    override fun read(statement: Table.() -> Query): Sequence<Structure> {
        val nodeTypePools = tableReader.read()

        return transaction {
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