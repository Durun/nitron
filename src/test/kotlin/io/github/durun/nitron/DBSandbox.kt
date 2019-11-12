package io.github.durun.nitron

import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.cpanalyzer.table.reader.BugfixRevisionsReader
import org.jetbrains.exposed.sql.selectAll
import java.nio.file.Paths

fun main() = test()

fun test() {
    val path = Paths.get("testdata/database/bugs.db")
    path.toFile().isFile
    println("DB file = ${path}")

    val db = SQLiteDatabase.connect(path)

    val seq = BugfixRevisionsReader(db).read { selectAll() }
    seq.forEachIndexed { i, it ->
        println("$i $it")
    }
    println(seq.count())
}
