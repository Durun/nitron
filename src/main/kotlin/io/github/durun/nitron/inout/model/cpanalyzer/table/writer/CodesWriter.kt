package io.github.durun.nitron.inout.model.cpanalyzer.table.writer

import io.github.durun.nitron.inout.model.cpanalyzer.Code
import io.github.durun.nitron.inout.model.cpanalyzer.table.Codes
import org.jetbrains.exposed.sql.Database

class CodesWriter(db: Database) : TableWriter<Code> by BufferedTableWriter(db, Codes, idColumn = Codes.id)