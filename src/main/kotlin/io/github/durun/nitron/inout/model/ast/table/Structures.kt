package io.github.durun.nitron.inout.model.ast.table

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.inout.model.ast.HashIndexedNode
import io.github.durun.nitron.inout.model.cpanalyzer.table.ReadWritableTable
import io.github.durun.nitron.inout.model.cpanalyzer.table.ReadableTable
import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.nio.ByteBuffer
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

object Structures : ReadWritableTable<HashIndexedNode>("structure_codes") {
    private val mapper = jacksonObjectMapper()

    val grammar: Column<String> = text("grammar")
    val idMsb: Column<Long> = long("id_msb").primaryKey()
    val idLsb: Column<Long> = long("id_lsb").primaryKey()
    val hash: Column<Blob> = blob("hash")
    val json: Column<String> = text("json")

    override fun write(value: HashIndexedNode, insertId: Long?): UpdateBuilder<Number> = insertIgnore {
        value.let { value ->
            val bytes = value.hash
            it[idMsb] = ByteBuffer.wrap(bytes, 0, 8).long
            it[idLsb] = ByteBuffer.wrap(bytes, 8, 8).long
            it[hash] = SerialBlob(bytes)
            it[json] = mapper.writeValueAsString(value.node)
            it[grammar] = value.grammar
        }
    }

    override fun read(row: ResultRow, alias: Alias<ReadableTable<HashIndexedNode>>?): HashIndexedNode {
        fun <T> Column<T>.get(): T = row[alias?.get(this) ?: this]
        val hash = hash.get().binaryStream.readAllBytes()
        val json = json.get()
        val grammar = grammar.get()
        return HashIndexedNode(node = mapper.readValue(json), hash = hash, grammar = grammar)
    }
}