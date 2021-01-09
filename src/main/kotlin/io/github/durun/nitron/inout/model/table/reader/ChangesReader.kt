package io.github.durun.nitron.inout.model.table.reader

import io.github.durun.nitron.inout.model.Change
import io.github.durun.nitron.inout.model.table.Changes
import io.github.durun.nitron.inout.model.table.Changes.afterCodes
import io.github.durun.nitron.inout.model.table.Changes.afterID
import io.github.durun.nitron.inout.model.table.Changes.beforeCodes
import io.github.durun.nitron.inout.model.table.Codes
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.selectAll

object ChangesReader : TableReader<Change> by BufferedTableReader(Changes) {
    override fun read(): Sequence<Change> = read {
        Changes
                .leftJoin(otherTable = beforeCodes, onColumn = { beforeID }, otherColumn = { beforeCodes[Codes.id] })
                .leftJoin(otherTable = afterCodes, onColumn = { afterID }, otherColumn = { afterCodes[Codes.id] })
                .selectAll()
    }
}