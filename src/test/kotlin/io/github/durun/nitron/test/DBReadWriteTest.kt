package io.github.durun.nitron.test

import io.github.durun.nitron.core.ast.basic.lineRangeOf
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.*
import io.github.durun.nitron.inout.model.table.Changes
import io.github.durun.nitron.inout.model.table.Codes
import io.github.durun.nitron.inout.model.table.reader.ChangesReader
import io.github.durun.nitron.inout.model.table.reader.CodesReader
import io.github.durun.nitron.inout.model.table.writer.ChangesWriter
import io.github.durun.nitron.inout.model.table.writer.CodesWriter
import io.kotlintest.properties.Gen
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.file.Paths
import java.util.*

class DBReadWriteTest : FreeSpec() {
    val path = Paths.get("testdata/database/test.db")
    val db = SQLiteDatabase.connect(path)

    init {
        "Codes" {
            transaction(db) {
                SchemaUtils.drop(Codes)
                SchemaUtils.create(Codes)
            }
            val writer = CodesWriter(db)
            val reader = CodesReader(db)
            codeGen.random().take(1000).toList().let { writeList ->
                writer.write(writeList)
                val readList = reader.read().toList()

                println("write: ${writeList.joinToString { "[${it.id}] ${it.rawText}" }}")
                println("read : ${readList.joinToString { "[${it.id}] ${it.rawText}" }}")

                readList.forEach {
                    writeList.contains(it) shouldBe true
                }
            }
        }
        "Changes" {
            transaction(db) {
                SchemaUtils.drop(Codes)
                SchemaUtils.create(Codes)
                SchemaUtils.drop(Changes)
                SchemaUtils.create(Changes)
            }
            val writer = ChangesWriter(db)
            val reader = ChangesReader(db)
            changeGen.random().take(1).toList().let { writeList ->
                writer.write(writeList)
                val readList = reader.read().toList()

                println("write: ${writeList.joinToString { "[${it.id}] ${it.beforeCode}" }}")
                println("read : ${readList.joinToString { "[${it.id}] ${it.beforeCode}" }}")
                println("${readList.first() == writeList.first()}")
                println("${readList.first().afterCode.hashCode()}, ${writeList.first().afterCode.hashCode()}")
                readList.forEach {
                    writeList.contains(it) shouldBe true
                }
            }
        }
    }

    val dateGen = Gen.long().map { Date(it) }
            .map { ammoniaDateFormat.format(it) }
            .map { ammoniaDateFormat.parse(it) }
    val codeGen = Gen.bind(
            Gen.string(), Gen.string(), Gen.int(), Gen.int()
    ) { soft, text, start, length ->
        Code(
                softwareName = soft,
                rawText = text,
                normalizedText = text,
                range = lineRangeOf(start, start + length)
        )
    }
    val changeGen = Gen.bind(
            Gen.string(), Gen.file().map(File::toPath), Gen.string(),
            Gen.pair(codeGen, codeGen),
            Gen.string(), dateGen,
            Gen.bind(Gen.enum<ChangeType>(), Gen.enum<DiffType>(), ::Pair)
    ) { soft, path, author, code, revision, date, type ->
        val codes = code.toList()
        CodesWriter(db).write(codes)
        val recordedCode = CodesReader(db).read()
                .filter { codes.contains(it) }.take(2).zipWithNext().first()
        Change(
                softwareName = soft,
                filePath = path,
                author = author,
                beforeCode = recordedCode.first,
                afterCode = recordedCode.second,
                commitHash = revision,
                date = date,
                changeType = type.first,
                diffType = type.second
        )
    }
}