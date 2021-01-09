package io.github.durun.nitron.app

import io.github.durun.nitron.core.ast.type.AstSerializers
import io.github.durun.nitron.inout.database.MemoryDatabase
import io.github.durun.nitron.inout.model.ast.table.NodeTypePools
import io.github.durun.nitron.inout.model.ast.table.Structures
import io.github.durun.nitron.nodeTypePool
import io.github.durun.nitron.structure
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class AstJsonImportCommandTest : FreeSpec({
	"writeAstJson" {
		val db = MemoryDatabase.connect("test")
		checkAll(iterations = 2,
				Arb.nodeTypePool(),
				Arb.int(2..10)
		) { typeSet, size ->
			val serializer = AstSerializers.json(typeSet)
			val asts = Arb.structure(typeSet).take(size)
			val ndjson = serializer.encodeToString(typeSet) + "\n" +
					asts.joinToString("\n") { serializer.encodeToString(it) }
			val input = ndjson.byteInputStream().bufferedReader()
			println("input:\n$ndjson")

			transaction(db) {
				db.writeAstJson(input)

				val (typesId, readTypeSet) = transaction {
					val row = NodeTypePools
							.select { NodeTypePools.grammar eq typeSet.grammar }
							.firstOrNull()
					row shouldNotBe null
					val id = row!![NodeTypePools.id]
					id to NodeTypePools.read(row)
				}
				readTypeSet shouldBe typeSet

				val readAsts = transaction {
					Structures.select { Structures.nodeTypeSet eq typesId }
							.map { row ->
								Structures.read(row, readTypeSet)
							}
				}
				readAsts.shouldContainExactly(asts)
			}
		}
	}
})

