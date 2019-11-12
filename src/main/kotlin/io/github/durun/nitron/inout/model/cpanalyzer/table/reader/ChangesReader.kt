package io.github.durun.nitron.inout.model.cpanalyzer.table.reader

import io.github.durun.nitron.inout.model.cpanalyzer.Change
import io.github.durun.nitron.inout.model.cpanalyzer.table.Changes
import io.github.durun.nitron.inout.model.cpanalyzer.table.Changes.afterCodes
import io.github.durun.nitron.inout.model.cpanalyzer.table.Changes.afterID
import io.github.durun.nitron.inout.model.cpanalyzer.table.Changes.beforeCodes
import io.github.durun.nitron.inout.model.cpanalyzer.table.Codes
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.selectAll

class ChangesReader(db: Database) : TableReader<Change> by BufferedTableReader(db, Changes) {
    override fun read(): Sequence<Change> = read {
        Changes
                .innerJoin(beforeCodes, { beforeID }, { beforeCodes[Codes.id] })
                .leftJoin(afterCodes, { afterID }, { afterCodes[Codes.id] })
                .selectAll()
    }
}