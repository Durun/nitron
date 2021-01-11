package io.github.durun.nitron.inout.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

object MemoryDatabase {
	private var count = 0
	fun connectNew(): Database = connect("${count++}")

	fun connect(name: String): Database {
		val db = Database.connect("jdbc:sqlite:file:$name?mode=memory&cache=shared", "org.sqlite.JDBC")
		TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
		return db
	}
}
