package io.github.durun.nitron.inout.model.cpanalyzer.table.reader

import io.github.durun.nitron.inout.model.cpanalyzer.Revision
import io.github.durun.nitron.inout.model.cpanalyzer.table.BugfixRevisions
import org.jetbrains.exposed.sql.Database

class BugfixRevisionsReader(db: Database)
    : TableReader<Revision> by BufferedTableReader(db, BugfixRevisions)