package io.github.durun.nitron.inout.model.cpanalyzer.table.writer

import io.github.durun.nitron.inout.model.cpanalyzer.Change
import io.github.durun.nitron.inout.model.cpanalyzer.table.Changes
import org.jetbrains.exposed.sql.Database

class ChangesWriter(db: Database) : TableWriter<Change> by BufferedTableWriter(db, Changes, idColumn = Changes.id)