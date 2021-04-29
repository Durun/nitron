package io.github.durun.nitron.app.db

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.toMD5
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.table.Changes
import io.github.durun.nitron.inout.model.table.Codes
import io.github.durun.nitron.util.Logger
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.nio.file.Path

/**
 * MPAnalyzerの出力するcodeテーブルの重複を正常化するプログラム
 */
class CleanCodeTableCommand : CliktCommand(name = "cleancodes") {
    private val dbFiles: List<Path> by argument(name = "DATABASE", help = "Database file")
        .path(writable = true)
        .multiple()

    private val log by logger()

    override fun run() {
        dbFiles.forEach {
            processOneDB(SQLiteDatabase.connect(it), log)
            log.info { "Done: $it" }
        }
    }

    companion object {
        fun processOneDB(db: Database, log: Logger? = null) {
            val fatHashes = detectFatHashes(db)
            fatHashes.forEach { (_, idSet) ->
                val newId = idSet.minOrNull()!!
                val oldIds = idSet - newId
                updateBeforeAfterId(db, oldIds, newId)
                deleteRedundantCodes(db, oldIds)
                log?.debug { "updated $idSet -> $newId" }
            }
        }

        private fun detectFatHashes(db: Database): Map<MD5, Set<Int>> {
            val hashes: MutableMap<MD5, MutableSet<Int>> = mutableMapOf()
            transaction(db) {
                Codes.selectAll()
                    .forEach {
                        val hash = it[Codes.hash].toMD5()
                        val id = it[Codes.id]
                        val idSet = hashes.computeIfAbsent(hash) { mutableSetOf() }
                        idSet.add(id)
                    }
            }
            return hashes.filterValues { 1 < it.size }
        }

        private fun updateBeforeAfterId(db: Database, oldIds: Set<Int>, newId: Int) {
            transaction(db) {
                Changes.update(where = { Changes.beforeID inList oldIds }) {
                    it[beforeID] = newId
                }
                Changes.update(where = { Changes.afterID inList oldIds }) {
                    it[afterID] = newId
                }
            }
        }

        private fun deleteRedundantCodes(db: Database, oldIds: Set<Int>) {
            transaction(db) {
                Codes.deleteWhere { Codes.id inList oldIds }
            }
        }
    }
}