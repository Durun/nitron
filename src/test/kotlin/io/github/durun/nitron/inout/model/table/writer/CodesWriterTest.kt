package io.github.durun.nitron.inout.model.table.writer

import io.github.durun.nitron.inout.database.MemoryDatabase
import io.github.durun.nitron.inout.model.code
import io.github.durun.nitron.inout.model.table.Codes
import io.github.durun.nitron.inout.model.table.reader.CodesReader
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.take
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class CodesWriterTest : FreeSpec({
	"write" {
		val db = MemoryDatabase.connect("test")
		val input = Arb.code().take(3).toList()
		val output = transaction(db) {
			SchemaUtils.create(Codes)
			CodesWriter(db).write(input)
			CodesReader(db).read().toList()
		}
		output shouldContainExactly input
	}
})
