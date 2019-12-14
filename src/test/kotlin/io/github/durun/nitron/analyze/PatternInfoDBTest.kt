package io.github.durun.nitron.analyze

import io.github.durun.nitron.analyze.db.PatternInfos
import io.github.durun.nitron.analyze.db.PatternInfosWriter
import io.github.durun.nitron.analyze.db.PatternReader
import io.github.durun.nitron.analyze.db.analyzeBy
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.kotlintest.TestCase
import io.kotlintest.matchers.sequences.shouldBeSameSizeAs
import io.kotlintest.matchers.sequences.shouldHaveAtLeastSize
import io.kotlintest.specs.FreeSpec
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths

class PatternInfoDBTest : FreeSpec() {
    val dir = Paths.get("testdata/database")
    val dbPath = dir.resolve("test.db")
    val db = SQLiteDatabase.connect(dbPath)

    override fun beforeTest(testCase: TestCase) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(PatternInfos)
            PatternInfos.deleteAll()
        }
    }

    init {
        "simple test" {
            val reader = PatternReader(db)
            val writer = PatternInfosWriter(db)

            val queries = listOf(TestQuery)

            println("reading...")
            val patterns = reader.read().take(10)
            patterns shouldHaveAtLeastSize 1
            println("read:")
            println(patterns.joinToString("\n") { it.hash.toString() })

            val results = patterns.analyzeBy(queries)

            println("writing...")
            writer.write(results)
            results shouldBeSameSizeAs patterns
            println("wrote: ")
            println(results.joinToString("\n") { "${it.pattern.hash}: ${it.getInfoString()}" })
        }
    }
}


object TestQuery : AnalyzeQuery<TestInfo> {
    override fun analyze(pattern: Pattern): TestInfo {
        return TestInfo
    }
}

object TestInfo : PatternInfo {
    override val name: String
        get() = "test"

    override fun getInfoString(): String = name
}