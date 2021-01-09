package io.github.durun.nitron.inout.model.table.writer

import io.github.durun.nitron.inout.model.Change
import io.github.durun.nitron.inout.model.table.Changes

object ChangesWriter : TableWriter<Change> by BufferedTableWriter(Changes, idColumn = Changes.id)