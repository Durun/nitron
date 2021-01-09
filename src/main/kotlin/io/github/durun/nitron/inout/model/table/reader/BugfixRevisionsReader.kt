package io.github.durun.nitron.inout.model.table.reader

import io.github.durun.nitron.inout.model.Revision
import io.github.durun.nitron.inout.model.table.BugfixRevisions

object BugfixRevisionsReader : TableReader<Revision> by BufferedTableReader(BugfixRevisions)