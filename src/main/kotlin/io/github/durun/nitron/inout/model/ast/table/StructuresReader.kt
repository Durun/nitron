package io.github.durun.nitron.inout.model.ast.table

import io.github.durun.nitron.inout.model.ast.HashIndexedNode
import io.github.durun.nitron.inout.model.cpanalyzer.table.reader.BufferedTableReader
import io.github.durun.nitron.inout.model.cpanalyzer.table.reader.TableReader
import org.jetbrains.exposed.sql.Database

class StructuresReader(db: Database) : TableReader<HashIndexedNode> by BufferedTableReader(db, Structures)