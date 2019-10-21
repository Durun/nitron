package io.github.durun.nitron.inout.model.table.reader

import io.github.durun.nitron.inout.model.Code
import io.github.durun.nitron.inout.model.table.Codes
import org.jetbrains.exposed.sql.Database

class CodesReader(db: Database) : TableReader<Code> by BufferedTableReader(db, Codes)