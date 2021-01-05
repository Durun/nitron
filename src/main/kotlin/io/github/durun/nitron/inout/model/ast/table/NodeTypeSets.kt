package io.github.durun.nitron.inout.model.ast.table

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.table.ReadWritableTable
import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement

@Deprecated("use NodeTypePool")
object NodeTypeSets : ReadWritableTable<NodeTypeSet>("node_type_sets") {

    val id: Column<Int> = integer("id")
            .autoIncrement("${tableName}_id")
            .primaryKey()
    val grammar: Column<String> = text("grammar")
    val tokenTypes: Column<String> = text("tokenTypes")
    val ruleNames: Column<String> = text("ruleNames")

    private val mapper = jacksonObjectMapper()

    override fun read(row: ResultRow): NodeTypeSet = read(row, null)

    fun read(row: ResultRow, alias: Alias<NodeTypeSets>?): NodeTypeSet {
        fun <T> Column<T>.get(): T = row[alias?.get(this) ?: this]
        val s = tokenTypes.get()
        return NodeTypeSet(
                grammar = grammar.get(),
                tokenTypes = mapper.readValue(s),
                ruleNames = mapper.readValue(ruleNames.get())
        )
    }

    override fun insert(value: NodeTypeSet, insertId: Int?): InsertStatement<Number> = insert {
        it[id] = insertId ?: getNextId(idColumn = id)
        it[grammar] = value.grammar
        it[tokenTypes] = mapper.writeValueAsString(value.tokenTypes)
        it[ruleNames] = mapper.writeValueAsString(value.ruleNames)
    }
}