package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.table.ReadWritableTable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement

object NodeTypePools : ReadWritableTable<NodeTypePool>("node_type_pools") {
	val id: Column<Int> = integer("id")
			.autoIncrement("${tableName}_id")
			.primaryKey()
	val grammar: Column<String> = text("grammar")
	val data: Column<String> = text("data")


	override fun read(row: ResultRow): NodeTypePool = read(row, null)

	fun read(row: ResultRow, alias: Alias<NodeTypePools>?): NodeTypePool {
		fun <T> Column<T>.get(): T = row[alias?.get(this) ?: this]
		return Json.decodeFromString(data.get())
	}

	override fun insert(value: NodeTypePool, insertId: Int?): InsertStatement<Number> = insert {
		it[id] = insertId ?: getNextId(idColumn = id)
		it[grammar] = value.grammar
		it[data] = Json.encodeToString(value)
	}
}