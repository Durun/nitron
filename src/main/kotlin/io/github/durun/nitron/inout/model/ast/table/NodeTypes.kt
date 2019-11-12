package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.inout.model.ast.NodeTypeMap
import io.github.durun.nitron.inout.model.cpanalyzer.table.ReadWritableTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.InsertStatement

object NodeTypes : ReadWritableTable<NodeTypeMap>("node_types") {

    val grammar: Column<String> = text("grammar")
            .primaryKey()
    val id: Column<Int> = integer("id")
            .primaryKey()
            .autoIncrement()
    val isTerminal: Column<Boolean> = bool("is_terminal")
    val index: Column<Int> = integer("index")
    val name: Column<String> = text("name")

    // TODO

    override fun insert(value: NodeTypeMap, insertId: Int?): InsertStatement<Number> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun read(row: ResultRow): NodeTypeMap {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}