package io.github.durun.nitron.test

import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.cpanalyzer.ChangeType
import io.github.durun.nitron.inout.model.cpanalyzer.Code
import io.github.durun.nitron.inout.model.cpanalyzer.table.reader.BugfixRevisionsReader
import io.github.durun.nitron.inout.model.cpanalyzer.table.reader.ChangesReader
import io.github.durun.nitron.inout.model.cpanalyzer.table.reader.CodesReader
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.FreeSpec
import java.nio.file.Paths

class DBReadTest : FreeSpec() {
    val path = Paths.get("testdata/database/bugs.db")
    val printSize = 4

    init {
        "read bug.db" - {
            "bugfixrevision table" {
                path.toFile().isFile shouldBe true

                println("DB file = $path")

                val db = SQLiteDatabase.connect(path)
                val reader = BugfixRevisionsReader(db)
                val seq = reader.read()
                seq.any {
                    it.softwareName == "ant" &&
                            it.commitHash == "282f346ca230a8dec8d1956af05fcc9d511ad672" &&
                            it.author == "Sam Ruby"
                } shouldBe true
                seq.take(printSize).forEach { println(it) }
            }
            "codes table" {
                val db = SQLiteDatabase.connect(path)
                val reader = CodesReader(db)
                val seq = reader.read()
                seq.take(printSize).forEach { println(it) }
                seq.forEach {
                    it.id shouldNotBe null
                }
            }
            "changes table" {
                val db = SQLiteDatabase.connect(path)
                val reader = ChangesReader(db)
                val seq = reader.read()
                seq.take(printSize).forEach { println(it) }
                seq.forEach {
                    it.id shouldNotBe null
                    when (it.changeType) {
                        ChangeType.CHANGE -> {
                            isNotEmpty(it.beforeCode) shouldBe true
                            isNotEmpty(it.afterCode) shouldBe true
                        }
                        ChangeType.ADD -> {
                            isNotEmpty(it.beforeCode) shouldBe false
                            isNotEmpty(it.afterCode) shouldBe true
                        }
                        ChangeType.DELETE -> {
                            isNotEmpty(it.beforeCode) shouldBe true
                            isNotEmpty(it.afterCode) shouldBe false
                        }
                    }
                }
            }
        }
    }

    private fun isNotEmpty(code: Code?) = code?.rawText?.isNotEmpty() ?: false
}