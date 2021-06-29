package io.github.durun.nitron.inout.model.preparse

import io.github.durun.nitron.core.config.LangConfig
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insertAndGetId

object LanguageTable : IntIdTable("languages") {
    val name: Column<String> = text("name")
    val checksum: Column<String> = varchar("checksum", 40)
        .uniqueIndex("checksum_unique")
}

@kotlin.io.path.ExperimentalPathApi
fun LanguageTable.insertAndGetId(langName: String, langConfig: LangConfig): EntityID<Int> {
    return insertAndGetId {
        it[this.name] = langName
        it[this.checksum] = langConfig.grammar.checksum().toString()
    }
}