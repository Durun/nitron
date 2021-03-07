package io.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.IntIdTable

object AstContentTable : IntIdTable("ast_contents") {
	val content = text("content")
	val checksum = varchar("checksum", 40)
		.uniqueIndex()
}