package com.github.durun.nitron.inout.model.table.writer

import com.github.durun.nitron.inout.model.Change
import com.github.durun.nitron.inout.model.table.Changes

object ChangesWriter : TableWriter<Change> by BufferedTableWriter(Changes, idColumn = Changes.id)