package io.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.IntIdTable

object AstTable : IntIdTable("asts") {
	val file = reference("file", FileTable)
		.uniqueIndex()
	val language = reference("language", LanguageTable)
	val content = optReference("content", AstContentTable)
}