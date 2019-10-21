package io.github.durun.nitron.inout.model.table.reader

import io.github.durun.nitron.inout.model.Revision
import io.github.durun.nitron.inout.model.table.BugfixRevisions
import org.jetbrains.exposed.sql.Database

class BugfixRevisionsReader(db: Database)
    : TableReader<Revision> by BufferedTableReader(db, BugfixRevisions)