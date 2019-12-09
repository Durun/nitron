package io.github.durun.nitron.analyze.db

import io.github.durun.nitron.core.toBlob
import io.github.durun.nitron.inout.model.table.ReadWritableTable
import io.github.durun.nitron.inout.model.table.writer.BufferedTableWriter
import io.github.durun.nitron.inout.model.table.writer.TableWriter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement

object PatternInfos : ReadWritableTable<PatternWithResult>("pattern_info") {
    val id = integer("id")
    val beforeHash = blob("beforeHash").primaryKey()
    val afterHash = blob("afterHash").primaryKey()
    val info = text("info")

    override fun insert(value: PatternWithResult, insertId: Int?): InsertStatement<Number> = insert {
        it[id] = getNextId(id)
        it[beforeHash] = value.pattern.hash.first.toBlob()
        it[afterHash] = value.pattern.hash.second.toBlob()
        it[info] = value.getInfoString()
    }

    override fun read(row: ResultRow): PatternWithResult = TODO()
}

class PatternInfosWriter(db: Database) : TableWriter<PatternWithResult> by BufferedTableWriter(db, PatternInfos, idColumn = PatternInfos.id)