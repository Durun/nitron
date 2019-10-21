package io.github.durun.nitron.data.model.table.writer

import io.github.durun.nitron.data.model.Change
import io.github.durun.nitron.data.model.table.Changes
import org.jetbrains.exposed.sql.Database

class ChangesWriter(db: Database) : TableWriter<Change> by BufferedTableWriter(db, Changes, idColumn = Changes.id)