package io.github.durun.nitron.inout.model.cpanalyzer.table.reader

import io.github.durun.nitron.inout.model.ast.table.Structures
import io.github.durun.nitron.inout.model.cpanalyzer.Code
import io.github.durun.nitron.inout.model.cpanalyzer.table.Codes
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.selectAll

class CodesReader(db: Database) : TableReader<Code> by BufferedTableReader(db, Codes) {
    override fun read(): Sequence<Code> = read {
        Codes
                .leftJoin(Structures, { hash }, { hash })
                .selectAll()
    }
}