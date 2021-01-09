package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.writer.BufferedTableWriter
import io.github.durun.nitron.inout.model.table.writer.TableWriter

object StructuresWriter : TableWriter<Structure> by BufferedTableWriter(Structures, idColumn = Structures.id, bufferSize = 1)