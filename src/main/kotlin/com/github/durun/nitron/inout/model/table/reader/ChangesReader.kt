package com.github.durun.nitron.inout.model.table.reader

import com.github.durun.nitron.inout.model.Change
import com.github.durun.nitron.inout.model.table.Changes
import com.github.durun.nitron.inout.model.table.Changes.afterCodes
import com.github.durun.nitron.inout.model.table.Changes.afterID
import com.github.durun.nitron.inout.model.table.Changes.beforeCodes
import com.github.durun.nitron.inout.model.table.Codes
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