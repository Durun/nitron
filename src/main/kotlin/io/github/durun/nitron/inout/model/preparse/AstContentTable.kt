package io.github.durun.nitron.inout.model.preparse

import io.github.durun.nitron.app.preparse.gzip
import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.toBlob
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.insertAndGetId
import java.sql.Blob

object AstContentTable : IntIdTable("ast_contents") {
	val content: Column<Blob> = blob("content")
	val checksum: Column<String> = varchar("checksum", 40)
		.uniqueIndex()
}

fun AstContentTable.insertAndGetId(content: String): EntityID<Int> {
	return insertAndGetId {
		it[this.content] = content.gzip().toBlob()
		it[this.checksum] = MD5.digest(content).toString()
	}
}