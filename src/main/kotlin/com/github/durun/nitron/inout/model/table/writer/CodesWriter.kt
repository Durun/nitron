package com.github.durun.nitron.inout.model.table.writer

import com.github.durun.nitron.inout.model.Code
import com.github.durun.nitron.inout.model.table.Codes

object CodesWriter : TableWriter<Code> by BufferedTableWriter(Codes, idColumn = Codes.id)