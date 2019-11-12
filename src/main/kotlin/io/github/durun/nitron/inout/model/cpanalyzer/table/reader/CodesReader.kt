package io.github.durun.nitron.inout.model.cpanalyzer.table.reader

import io.github.durun.nitron.inout.model.cpanalyzer.Code
import io.github.durun.nitron.inout.model.cpanalyzer.table.Codes
import org.jetbrains.exposed.sql.Database

class CodesReader(db: Database) : TableReader<Code> by BufferedTableReader(db, Codes)