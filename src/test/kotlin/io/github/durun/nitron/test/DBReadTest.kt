package io.github.durun.nitron.test

import io.github.durun.nitron.data.model.table.BugfixRevisions
import io.github.durun.nitron.data.model.table.reader.BugfixRevisionsReader
import io.github.durun.nitron.data.database.SQLiteDatabase
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import org.jetbrains.exposed.sql.selectAll
import java.nio.file.Paths

class DBReadTest: FreeSpec() {
    init {
        "read bug.db" {
            val path = Paths.get("testdata/database/bugs.db")
            path.toFile().isFile shouldBe true

            println("DB file = $path")

            val db = SQLiteDatabase.connect(path)
            val reader = BugfixRevisionsReader(db)
            val seq = reader.toSequence()
            seq.any {
                it.softwareName == "ant" &&
                        it.commitHash == "282f346ca230a8dec8d1956af05fcc9d511ad672" &&
                        it.author == "Sam Ruby"
            } shouldBe true
            seq.take(4).forEach { println(it) }
        }
    }
}