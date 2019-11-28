package io.github.durun.nitron.inout.model.ast.table

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.core.toBlob
import io.github.durun.nitron.core.toBytes
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.ast.SerializableAst
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.ReadWritableTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Blob

object Structures : ReadWritableTable<Structure>("structures") {
    val id = integer("id")
            .primaryKey()
    val hash: Column<Blob> = blob("hash")
            .primaryKey()
    val json: Column<String> = text("json")

    val nodeTypeSets = NodeTypeSets.alias("t")
    val nodeTypeSet = reference("nodeTypeSet", NodeTypeSets.id)

    private val mapper = jacksonObjectMapper()
    private fun SerializableAst.Node.writeAsString(): String = mapper.writeValueAsString(this)

    internal fun read(row: ResultRow, nodeTypeSet: NodeTypeSet): Structure {
        return Structure(
                nodeTypeSet = nodeTypeSet,
                ast = mapper.readValue(row[json]),
                hash = row[hash].toBytes()
        )
    }

    override fun read(row: ResultRow): Structure {
        assert(row.hasValue(nodeTypeSets[NodeTypeSets.id]))
        return read(row, NodeTypeSets.read(row, nodeTypeSets))
    }

    override fun insert(value: Structure, insertId: Int?): InsertStatement<Number> = insert {
        it[id] = insertId ?: getNextId(idColumn = id)
        it[hash] = value.hash.toBlob()
        it[json] = value.ast.writeAsString()
        value.nodeTypeSet.grammar
        val newId = transaction { NodeTypeSets.select { NodeTypeSets.grammar eq value.nodeTypeSet.grammar } }   // TODO refactor
                .firstOrNull()
                ?.getOrNull(NodeTypeSets.id)
                ?: let {
                    val newId = NodeTypeSets.getNextId(NodeTypeSets.id)
                    NodeTypeSets.insert(value.nodeTypeSet, newId)
                    newId
                }
        it[nodeTypeSet] = newId
    }
}