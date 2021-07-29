package com.github.durun.nitron.inout.model.table.reader

import com.github.durun.nitron.inout.model.Code
import com.github.durun.nitron.inout.model.table.Codes

object CodesReader : TableReader<Code> by BufferedTableReader(Codes)