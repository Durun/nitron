package com.github.durun.nitron.app.preparse

import com.github.durun.nitron.app.main
import com.github.durun.nitron.inout.database.SQLiteDatabase
import com.github.durun.nitron.inout.model.preparse.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.createTempDirectory

private fun execCommand(line: String) = main(line.split(" ").toTypedArray())
	.also { println("Executed $line") }

@kotlin.io.path.ExperimentalPathApi
class ParseCommandTest : FreeSpec({
	"example" {
        val dir = createTempDirectory("cache")
            .apply { toFile().deleteOnExit() }
        println("Created temp directory: $dir")

        val db = dir.resolve("cache.db")
        println("Created temp database: $db")

        execCommand("preparse-register --remote https://github.com/githubtraining/hellogitworld --lang java $db")
        transaction(SQLiteDatabase.connect(db)) {
            RepositoryTable.selectAll() shouldHaveSize 1
            RepositoryTable.selectAll().first()
        }.let {
            it[RepositoryTable.name] shouldBe "githubtraining/hellogitworld.git"
            it[RepositoryTable.url] shouldBe "https://github.com/githubtraining/hellogitworld.git"
            it[RepositoryTable.langs] shouldBe "java"
        }

        execCommand("preparse-fetch --branch master --dir $dir $db")
        transaction(SQLiteDatabase.connect(db)) {
            CommitTable.selectAll()
                .onEach {
                    it[CommitTable.repository].value shouldBe 1
                } shouldHaveSize 55
            FileTable.selectAll() shouldHaveSize 20
        }

        execCommand("preparse --repository https://github.com/githubtraining/hellogitworld --dir $dir $db")
        transaction(SQLiteDatabase.connect(db)) {
            AstTable.selectAll() shouldHaveSize 20
            AstTable.innerJoin(FileTable)
                .innerJoin(CommitTable)
                .select {
                    CommitTable.hash eq "ec0e235e87ee1125f781530cba61eee1188d0dfb" and
                            (FileTable.path eq "src/test/java/com/ambientideas/AppTest.java")
                }
                .first()
                .let {
                    it[AstTable.content]?.value shouldBe -1
                }
            AstContentTable.selectAll() shouldHaveSize 17
        }
    }

    "specify date range" {
        val dir = createTempDirectory("cache")
            .apply { toFile().deleteOnExit() }
        println("Created temp directory: $dir")

        val db = dir.resolve("cache.db")
        println("Created temp database: $db")

        execCommand("preparse-register --remote https://github.com/githubtraining/hellogitworld --lang java $db")
        transaction(SQLiteDatabase.connect(db)) {
            RepositoryTable.selectAll() shouldHaveSize 1
            RepositoryTable.selectAll().first()
        }.let {
            it[RepositoryTable.name] shouldBe "githubtraining/hellogitworld.git"
            it[RepositoryTable.url] shouldBe "https://github.com/githubtraining/hellogitworld.git"
            it[RepositoryTable.langs] shouldBe "java"
        }

        execCommand("preparse-fetch --branch master --start-date 01:01:2012 --end-date 31:12:2013 --dir $dir $db")
        transaction(SQLiteDatabase.connect(db)) {
            CommitTable.selectAll() shouldHaveSize 8
            FileTable.selectAll() shouldHaveSize 4
        }

        execCommand("preparse --repository https://github.com/githubtraining/hellogitworld --start-date 01:01:2012 --end-date 31:12:2013 --dir $dir $db")
        transaction(SQLiteDatabase.connect(db)) {
            AstTable.selectAll() shouldHaveSize 4
            AstContentTable.selectAll() shouldHaveSize 4
        }

        // rerun
        execCommand("preparse-fetch --branch master --dir $dir $db")
        transaction(SQLiteDatabase.connect(db)) {
            CommitTable.selectAll() shouldHaveSize 55
            FileTable.selectAll() shouldHaveSize 20
        }
        execCommand("preparse --repository https://github.com/githubtraining/hellogitworld --start-date 01:01:2012 --end-date 31:12:2013 --dir $dir $db")
        transaction(SQLiteDatabase.connect(db)) {
            AstTable.selectAll() shouldHaveSize 4
            AstContentTable.selectAll() shouldHaveSize 4
        }
        execCommand("preparse --repository https://github.com/githubtraining/hellogitworld --dir $dir $db")
        transaction(SQLiteDatabase.connect(db)) {
            AstTable.selectAll() shouldHaveSize 20
            AstContentTable.selectAll() shouldHaveSize 17
        }
    }
})