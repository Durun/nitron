package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.inout.model.ast.HashIndexedNode
import io.github.durun.nitron.inout.model.cpanalyzer.table.writer.BufferedTableWriter
import io.github.durun.nitron.inout.model.cpanalyzer.table.writer.TableWriter
import org.jetbrains.exposed.sql.Database

class StructuresWriter(db: Database) : TableWriter<HashIndexedNode> by BufferedTableWriter(db, Structures, idColumn = Structures.idMsb)