package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.core.ast.type.AstSerializers
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.toBlob
import io.github.durun.nitron.core.toBytes
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.ReadWritableTable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    val nodeTypePools = NodeTypePools.alias("t")
    val nodeTypeSet = reference("node_type_pool", NodeTypePools.id)


    fun read(row: ResultRow, nodeTypePool: NodeTypePool): Structure {
        return Structure(
                nodeTypePool = nodeTypePool,
                asts = AstSerializers.json(nodeTypePool).decodeFromString(row[json]),
                hash = row[hash].toBytes()
        )
    }

    override fun read(row: ResultRow): Structure {
        assert(row.hasValue(nodeTypePools[NodeTypePools.id]))
        return read(row, NodeTypePools.read(row, nodeTypePools))
    }

    override fun insert(value: Structure, insertId: Int?): InsertStatement<Number> = insert {
        it[id] = insertId ?: getNextId(idColumn = id)
        it[hash] = value.hash.toBlob()
        it[json] = Json.encodeToString(value.asts)
        value.nodeTypePool.grammar
        val newId = transaction { NodeTypePools.select { NodeTypePools.grammar eq value.nodeTypePool.grammar } }   // TODO refactor
                .firstOrNull()
                ?.getOrNull(NodeTypePools.id)
                ?: let {
                    val newId = NodeTypePools.getNextId(NodeTypePools.id)
                    NodeTypePools.insert(value.nodeTypePool, newId)
                    newId
                }
        it[nodeTypeSet] = newId
    }

    fun batchInsertRawValues(typeSetId: Int, hashAndJsons: Iterable<Pair<ByteArray, String>>): List<ResultRow> =
            batchInsert(hashAndJsons, ignore = true) { (hashValue, jsonValue) ->
                this[hash] = hashValue.toBlob()
                this[json] = jsonValue
                this[nodeTypeSet] = typeSetId
            }
}