package io.github.durun.nitron.inout.model.ast.structure.simple

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.durun.nitron.core.toBlob
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.sql.Blob

private val mapper = jacksonObjectMapper()
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
            SchemaUtils.createMissingTablesAndColumns(NodeTypeSets, Structures)
        }
    }

    fun writeBuffering(typeSet: NodeTypeSet, nodeSequence: Sequence<Pair<ByteArray, String>>) {
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

    private fun writeIfNotExist(typeSet: NodeTypeSet): Int {
        return transaction {
            NodeTypeSets
                    .select { NodeTypeSets.grammar eq typeSet.grammar }
                    .map { it[NodeTypeSets.id] }
                    .firstOrNull()
                    ?: kotlin.runCatching {
                        transaction { NodeTypeSets.insertValue(typeSet) }
                        NodeTypeSets
                                .select { NodeTypeSets.grammar eq typeSet.grammar }
                                .map { it[NodeTypeSets.id] }
                                .first()
                    }.getOrThrow()
        }
    }
}


private object NodeTypeSets : Table("node_type_sets") {
    val id: Column<Int> = integer("id").autoIncrement("${tableName}_id").primaryKey()
    val grammar: Column<String> = text("grammar")
    val tokenTypes: Column<String> = text("tokenTypes")
    val ruleNames: Column<String> = text("ruleNames")

    fun insertValue(value: NodeTypeSet): Statement<Int> = insert {
        it[grammar] = value.grammar
        it[tokenTypes] = mapper.writeValueAsString(value.tokenTypes)
        it[ruleNames] = mapper.writeValueAsString(value.ruleNames)
    }
}


private object Structures : Table("structures") {
    val hash: Column<Blob> = blob("hash").primaryKey()
    val json: Column<String> = text("json")
    val nodeTypeSet = reference("nodeTypeSet", NodeTypeSets.id)

    fun batchInsertRawValues(typeSetId: Int, hashAndJsons: Iterable<Pair<ByteArray, String>>): List<ResultRow> =
            batchInsert(hashAndJsons, ignore = true) { (hashValue, jsonValue) ->
                this[hash] = hashValue.toBlob()
                this[json] = jsonValue
                this[nodeTypeSet] = typeSetId
            }
}