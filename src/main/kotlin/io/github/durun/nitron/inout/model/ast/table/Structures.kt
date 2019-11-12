package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.inout.model.ast.Node
import io.github.durun.nitron.inout.model.cpanalyzer.table.Codes
import io.github.durun.nitron.inout.model.cpanalyzer.table.ReadWritableTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.InsertStatement

object Structures : ReadWritableTable<Node>("structures") {

    val grammar: Column<String> = text("grammar")
            .primaryKey()
    val codeId: Column<Int> = integer("code_id")
            .references(Codes.id)
    val json: Column<String> = text("json")

    // TODO

    override fun insert(value: Node, insertId: Int?): InsertStatement<Number> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun read(row: ResultRow): Node {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}