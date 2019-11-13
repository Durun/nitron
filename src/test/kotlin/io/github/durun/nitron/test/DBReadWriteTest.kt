package io.github.durun.nitron.test

import io.github.durun.nitron.core.ast.node.lineRangeOf
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.ast.HashIndexedNode
import io.github.durun.nitron.inout.model.ast.TerminalNode
import io.github.durun.nitron.inout.model.ast.table.Structures
import io.github.durun.nitron.inout.model.ast.table.StructuresReader
import io.github.durun.nitron.inout.model.ast.table.StructuresWriter
import io.github.durun.nitron.inout.model.cpanalyzer.*
import io.github.durun.nitron.inout.model.cpanalyzer.table.Changes
import io.github.durun.nitron.inout.model.cpanalyzer.table.Codes
import io.github.durun.nitron.inout.model.cpanalyzer.table.reader.ChangesReader
import io.github.durun.nitron.inout.model.cpanalyzer.table.reader.CodesReader
import io.github.durun.nitron.inout.model.cpanalyzer.table.writer.ChangesWriter
import io.github.durun.nitron.inout.model.cpanalyzer.table.writer.CodesWriter
import io.kotlintest.properties.Gen
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*

class DBReadWriteTest : FreeSpec() {
    val path = Paths.get("testdata/database/test.db")
    val db = SQLiteDatabase.connect(path)

    init {
        "Structures" {
            transaction(db) {
                SchemaUtils.drop(Structures)
                SchemaUtils.create(Structures)
            }
            val writer = StructuresWriter(db)
            val reader = StructuresReader(db)
            structureGen.random().take(100).toList().let { writeList ->
                writer.write(writeList)
                val readList = reader.read().toList()

                println("write: ${writeList.joinToString { it.toString() }}")
                println("\nread : ${readList.joinToString { it.toString() }}")

                readList.forEach {
                    writeList.contains(it) shouldBe true
                }
            }
        }
        "Codes" {
            transaction(db) {
                SchemaUtils.drop(Structures)
                SchemaUtils.create(Structures)
                SchemaUtils.drop(Codes)
                SchemaUtils.create(Codes)
            }
            val writer = CodesWriter(db)
            val reader = CodesReader(db)
            codeGen.random().take(100).toList().let { writeList ->
                println("write: ${writeList.joinToString { "[${it.id}] ${it.structure} ${it.rawText}" }}")
                writer.write(writeList)
                val readList = reader.read().toList()
                println("\nread : ${readList.joinToString { "[${it.id}] ${it.structure} ${it.rawText}" }}")

                readList.forEach {
                    writeList.contains(it) shouldBe true
                }
            }
        }
        "Changes" {
            transaction(db) {
                SchemaUtils.drop(Structures)
                SchemaUtils.create(Structures)
                SchemaUtils.drop(Codes)
                SchemaUtils.create(Codes)
                SchemaUtils.drop(Changes)
                SchemaUtils.create(Changes)
            }
            val writer = ChangesWriter(db)
            val reader = ChangesReader(db)
            changeGen.random().take(100).toList().let { writeList ->
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

    val nodeGen = Gen.bind(
            Gen.int()
    ) { type -> TerminalNode(type = type, text = "term") }
    val structureGen = Gen.bind(
            nodeGen, Gen.string(), Gen.string()
    ) { node, str, grammar ->
        HashIndexedNode(
                node,
                MessageDigest.getInstance("MD5").digest(str.toByteArray()),
                grammar
        )
    }

    val codeGen = Gen.bind(
            Gen.string(), Gen.string(), Gen.int(), Gen.int(), nodeGen
    ) { soft, text, start, length, node ->
        val code = Code(
                softwareName = soft,
                rawText = text,
                normalizedText = text,
                range = lineRangeOf(start, start + length),
                structure = node
        )
        StructuresWriter(db).write(
                HashIndexedNode(code.structure!!, code.hash, grammar = code.softwareName)
        )
        code
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