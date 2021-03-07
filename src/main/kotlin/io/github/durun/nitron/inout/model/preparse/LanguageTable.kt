package io.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column

object LanguageTable : IntIdTable("languages") {
	val name: Column<String> = text("name")
	val checksum: Column<String> = varchar("checksum", 40)
		.uniqueIndex("checksum_unique")
}