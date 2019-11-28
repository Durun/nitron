package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.writer.BufferedTableWriter
import io.github.durun.nitron.inout.model.table.writer.TableWriter
import org.jetbrains.exposed.sql.Database

class StructuresWriter(db: Database) : TableWriter<Structure> by BufferedTableWriter(db, Structures, idColumn = Structures.id, bufferSize = 1)