package io.github.durun.nitron.test

import io.github.durun.nitron.core.ast.node.lineRangeOf
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.*
import io.github.durun.nitron.inout.model.table.Changes
import io.github.durun.nitron.inout.model.table.Codes
import io.github.durun.nitron.inout.model.table.reader.ChangesReader
import io.github.durun.nitron.inout.model.table.reader.CodesReader
import io.github.durun.nitron.inout.model.table.writer.ChangesWriter
import io.github.durun.nitron.inout.model.table.writer.CodesWriter
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.file.Paths
import java.util.*

class DBReadWriteTest : FreeSpec() {
    val path = Paths.get("testdata/database/test.db")
    val db = SQLiteDatabase.connect(path)

    init {
        "Changes" {
            transaction(db) {
                SchemaUtils.drop(Codes)
                SchemaUtils.create(Codes)
                SchemaUtils.drop(Changes)
                SchemaUtils.create(Changes)
            }
            val writer = ChangesWriter(db)
            val reader = ChangesReader(db)
            Arb.change().take(3).toList().let { writeList ->
                writeList.forEach {
                    CodesWriter(db).write(listOfNotNull(it.beforeCode, it.afterCode))
                }
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

    val dateGen = Arb.localDateTime()
    val codeGen = Arb.bind(
            Arb.string(), Arb.string(), Arb.int(), Arb.int()
    ) { soft, text, start, length ->
        Code(
                softwareName = soft,
                rawText = text,
                normalizedText = text,
                range = lineRangeOf(start, start + length)
        )
    }
    val changeGen = Arb.bind(
            Arb.string(), Arb.file().map(File::toPath), Arb.string(),
            Arb.pair(codeGen, codeGen),
            Arb.string(), dateGen,
            Arb.bind(Arb.enum<ChangeType>(), Arb.enum<DiffType>(), ::Pair)
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