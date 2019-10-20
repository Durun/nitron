package io.github.durun.nitron.data.database

import org.jetbrains.exposed.sql.Database
import java.nio.file.Path
import java.sql.Driver
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal class FileDatabaseBase(
    private val driver: KClass<out Driver>,
    private val urlMap: (Path) -> String,
    private val patch: () -> Unit = {}
): FileDatabase {
    override fun connect(path: Path): Database {
        val db = Database.connect(urlMap(path), driver.jvmName)
        patch()
        return db
    }
}