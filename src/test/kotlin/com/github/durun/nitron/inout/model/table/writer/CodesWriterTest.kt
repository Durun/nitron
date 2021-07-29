package com.github.durun.nitron.inout.model.table.writer

import com.github.durun.nitron.inout.database.MemoryDatabase
import com.github.durun.nitron.inout.model.code
import com.github.durun.nitron.inout.model.table.Codes
import com.github.durun.nitron.inout.model.table.reader.CodesReader
import io.kotest.core.spec.DoNotParallelize
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.property.Arb
import io.kotest.property.arbitrary.take
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@DoNotParallelize
class CodesWriterTest : FreeSpec({
	"write" {
		val db = MemoryDatabase.connectNew()
		val input = Arb.code().take(3).toList()
		val output = transaction(db) {
			SchemaUtils.create(Codes)
			CodesWriter.write(input)
			CodesReader.read().toList()
		}
		output shouldContainExactly input
	}
})
