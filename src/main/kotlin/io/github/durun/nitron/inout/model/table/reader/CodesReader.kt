package io.github.durun.nitron.inout.model.table.reader

import io.github.durun.nitron.inout.model.Code
import io.github.durun.nitron.inout.model.table.Codes

object CodesReader : TableReader<Code> by BufferedTableReader(Codes)