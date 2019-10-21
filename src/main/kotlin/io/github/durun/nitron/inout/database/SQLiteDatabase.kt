package io.github.durun.nitron.inout.database

import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.sqlite.JDBC
import java.sql.Connection

object SQLiteDatabase :
        FileDatabase by FileDatabaseBase(
                JDBC::class,
                { "jdbc:sqlite:$it" },
                { TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE }
        )