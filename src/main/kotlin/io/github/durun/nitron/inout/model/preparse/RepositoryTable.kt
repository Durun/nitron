package io.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column

object RepositoryTable : IntIdTable("repositories") {
	val name: Column<String> = text("name")
	val url: Column<String> = text("url")
		.uniqueIndex("url")
	val langs: Column<String> = text("languages")    // comma delimited language names
}