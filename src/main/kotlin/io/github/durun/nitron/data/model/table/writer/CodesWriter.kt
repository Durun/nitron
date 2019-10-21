package io.github.durun.nitron.data.model.table.writer

import io.github.durun.nitron.data.model.Code
import io.github.durun.nitron.data.model.table.Codes
import org.jetbrains.exposed.sql.Database

class CodesWriter(db: Database) : TableWriter<Code> by BufferedTableWriter(db, Codes, idColumn = Codes.id)