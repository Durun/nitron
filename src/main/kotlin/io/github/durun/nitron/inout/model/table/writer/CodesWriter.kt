package io.github.durun.nitron.inout.model.table.writer

import io.github.durun.nitron.inout.model.Code
import io.github.durun.nitron.inout.model.table.Codes
import org.jetbrains.exposed.sql.Database

class CodesWriter(db: Database) : TableWriter<Code> by BufferedTableWriter(db, Codes, idColumn = Codes.id)