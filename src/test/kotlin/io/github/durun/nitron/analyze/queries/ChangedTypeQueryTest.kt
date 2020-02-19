package io.github.durun.nitron.analyze.queries

import io.github.durun.nitron.analyze.AnalyzeContext
import io.github.durun.nitron.analyze.contexts.ifAfter
import io.github.durun.nitron.analyze.contexts.ifAfterContains
import io.github.durun.nitron.analyze.contexts.ifChanged
import io.github.durun.nitron.analyze.contexts.ifIntroduced
import io.github.durun.nitron.analyze.db.*
import io.github.durun.nitron.analyze.message.means
import io.github.durun.nitron.analyze.node.any
import io.github.durun.nitron.analyze.node.filterOnce
import io.github.durun.nitron.analyze.query.FirstQuery
import io.github.durun.nitron.analyze.query.or
import io.github.durun.nitron.analyze.query.reversed
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.ast.SerializableAst
import io.github.durun.nitron.inout.model.ast.table.NodeTypePoolReader
import io.kotlintest.TestCase
import io.kotlintest.specs.FreeSpec
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths

class ChangedTypeQueryTest : FreeSpec() {
    val dir = Paths.get("databases")
    val dbPath = dir.resolve("apache.db")
    val db = SQLiteDatabase.connect(dbPath)

    override fun beforeTest(testCase: TestCase) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(PatternInfos)
            PatternInfos.deleteAll()
        }
    }

    init {
        "detect test" {

            val reader = PatternReader(db)
            val writer = PatternInfosWriter(db)


            println("reading...")
            val patterns = reader.read()
            println("read.")

            val queries = AnalyzeContext(
                    types = NodeTypePoolReader(db).readById(1)
            ).scope {
                val basicFor_to_enhancedFor = (
                        ifChanged(type = "basicForStatement" to "enhancedForStatement")
                                or ifChanged(type = "basicForStatementNoShortIf" to "enhancedForStatementNoShortIf"))
                val containStreamForEach = { it: SerializableAst.Node ->
                    it.filterOnce { node ->
                        node.type == typeOf("methodInvocation").index
                                && node.any { it.type == typeOf("Identifier").index && it.text == "forEach" }
                    }.count() > 0
                }
                listOf(
                        FirstQuery.of(
                                "with-with" means ifChanged(type = "tryWithResourcesStatement" to "tryWithResourcesStatement"),
                                "try-with" means ifChanged(type = "tryStatement" to "tryWithResourcesStatement"),
                                "with-try" means ifChanged(type = "tryWithResourcesStatement" to "tryStatement")
                        ),
                        FirstQuery.of(
                                "-with" means ifAfterContains(type = "tryWithResourcesStatement"),
                                "-try" means ifAfterContains(type = "tryStatement")
                        ),
                        "for-foreach" means basicFor_to_enhancedFor,
                        "foreach-for" means basicFor_to_enhancedFor.reversed(),
                        FirstQuery.of(
                                "-foreach" means (ifIntroduced(type = "enhancedForStatementNoShortIf") or ifIntroduced(type = "enhancedForStatementNoShortIf")),
                                "-?foreach" means (ifAfterContains(type = "enhancedForStatementNoShortIf") or ifAfterContains(type = "enhancedForStatementNoShortIf"))
                        ),
                        FirstQuery.of(
                                "-for" means (ifIntroduced(type = "basicForStatement") or ifIntroduced(type = "basicForStatementNoShortIf")),
                                "-?for" means (ifAfterContains(type = "basicForStatement") or ifAfterContains(type = "basicForStatementNoShortIf"))
                        ),
                        "-streamForEach" means ifAfter(containStreamForEach)
                        /*,
                                "cond-intro-strict-fromIf" means (
                                        ifInherit(
                                                to = "conditionalExpression",
                                                types = arrayOf("conditionalOrExpression", "QUESTION", "expression", "COLON", "conditionalExpression"),
                                                requireInherited = arrayOf(true, false, true, false, true))
                                        and ifBeforeContains("QUESTION").not()
                                        and (ifBeforeContains("ifThenElseStatement") or ifBeforeContains("ifThenElseStatementNoShortIf"))),
                                "cond-intro-fromIf" means (
                                        (ifBeforeContains("ifThenElseStatement") or ifBeforeContains("ifThenElseStatementNoShortIf"))
                                                and ifAfterContains("conditionalExpression", "QUESTION")),
                                "cond-intro" means ifIntroduced("conditionalExpression", "QUESTION"),
                                "support-fromIf" means (ifBeforeContains("ifThenElseStatement") or ifBeforeContains("ifThenElseStatementNoShortIf"))
                        )
                         */
                )
            }
            val results: Sequence<PatternWithResult> = patterns.analyzeBy(queries)

            println("writing...")
            writer.write(results)
            println("wrote.")
        }
    }
}