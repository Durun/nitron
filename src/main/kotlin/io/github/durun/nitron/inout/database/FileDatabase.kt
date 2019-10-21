package io.github.durun.nitron.inout.database

import org.jetbrains.exposed.sql.Database
import java.nio.file.Path

interface FileDatabase {
    fun connect(path: Path): Database
}