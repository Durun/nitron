package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.inout.model.ast.NodeTypeMap
import io.github.durun.nitron.inout.model.cpanalyzer.table.ReadWritableTable
import io.github.durun.nitron.inout.model.cpanalyzer.table.ReadableTable
import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement

object NodeTypes : ReadWritableTable<NodeTypeEntry>("structure_types") {

    val grammar: Column<String> = text("grammar")
            .primaryKey()
    val id: Column<Long> = long("id")
            .primaryKey()
            .autoIncrement()
    val isTerminal: Column<Boolean> = bool("is_terminal")
    val index: Column<Int> = integer("index")
    val name: Column<String> = text("name")

    override fun write(value: NodeTypeEntry, insertId: Long?): InsertStatement<Number> = insert {
        if (insertId != null) it[id] = insertId
        it[grammar] = value.grammar
        it[isTerminal] = (value is TerminalNodeTypeEntry)
        it[index] = value.index
        it[name] = value.name
    }

    override fun read(row: ResultRow, alias: Alias<ReadableTable<NodeTypeEntry>>?): NodeTypeEntry {
        fun <T> Column<T>.get(): T = row[alias?.get(this) ?: this]
        return if (isTerminal.get()) {
            TerminalNodeTypeEntry(
                    grammar = grammar.get(),
                    index = index.get(),
                    name = name.get()
            )
        } else {
            RuleNodeTypeEntry(
                    grammar = grammar.get(),
                    index = index.get(),
                    name = name.get()
            )
        }
    }

    fun read(entries: Collection<NodeTypeEntry>): NodeTypeMap {
        val grammar = entries.first().grammar   // TODO
        val sorted = entries
                .map { Pair(it.index, it.name) }
                .sortedBy { it.first }
                .map { it.second }
        val tokens = sorted.filterIsInstance<TerminalNodeTypeEntry>().map { it.name }
        val rules = sorted.filterIsInstance<RuleNodeTypeEntry>().map { it.name }
        return NodeTypeMap(tokens.toTypedArray(), rules.toTypedArray(), grammar)
    }
}

abstract class NodeTypeEntry(
        val grammar: String,
        val index: Int,
        val name: String
)

class RuleNodeTypeEntry(
        grammar: String,
        index: Int,
        name: String
) : NodeTypeEntry(grammar, index, name)

class TerminalNodeTypeEntry(
        grammar: String,
        index: Int,
        name: String
) : NodeTypeEntry(grammar, index, name)