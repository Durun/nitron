package io.github.durun.nitron.inout.model.table.writer

import io.github.durun.nitron.inout.model.Code
import io.github.durun.nitron.inout.model.table.Codes

object CodesWriter : TableWriter<Code> by BufferedTableWriter(Codes, idColumn = Codes.id)