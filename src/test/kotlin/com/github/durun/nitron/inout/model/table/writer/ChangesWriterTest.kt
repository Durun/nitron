package com.github.durun.nitron.inout.model.table.writer

import com.github.durun.nitron.inout.database.MemoryDatabase
import com.github.durun.nitron.inout.model.change
import com.github.durun.nitron.inout.model.table.Changes
import com.github.durun.nitron.inout.model.table.Codes
import com.github.durun.nitron.inout.model.table.reader.ChangesReader
import io.kotest.core.spec.DoNotParallelize
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.property.Arb
import io.kotest.property.arbitrary.take
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@DoNotParallelize
class ChangesWriterTest : FreeSpec({
	"write" {
		val db = MemoryDatabase.connectNew()
		val input = Arb.change().take(10).toList()
		val output = transaction(db) {
			SchemaUtils.create(Codes, Changes)
			input.forEach {
				CodesWriter.write(listOfNotNull(it.beforeCode, it.afterCode))
			}
			ChangesWriter.write(input)
			ChangesReader.read().toList()
		}
		output shouldContainExactly input
	}
})
