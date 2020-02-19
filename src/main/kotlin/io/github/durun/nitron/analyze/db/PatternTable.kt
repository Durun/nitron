package io.github.durun.nitron.analyze.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.analyze.Pattern
import io.github.durun.nitron.core.toBytes
import io.github.durun.nitron.inout.model.ast.table.Structures
import io.github.durun.nitron.inout.model.table.ReadableTable
import io.github.durun.nitron.inout.model.table.reader.BufferedTableReader
import io.github.durun.nitron.inout.model.table.reader.TableReader
import org.jetbrains.exposed.sql.*

private val mapper = jacksonObjectMapper()

object Patterns : ReadableTable<Pattern>(name = "patterns") {
    val id = integer("id").primaryKey()
    val beforeHash = blob("beforeHash")
    val afterHash = blob("afterHash")
    val changeType = integer("changetype")
    val diffType = integer("difftype")
    val support = integer("support")
    val confidence = double("confidence")
    val authors = integer("authors")
    val files = integer("files")
    val nos = integer("nos")
    val firstDate = text("firstdate")
    val lastDate = text("lastdate")

    val beforeStructuresAlias = Structures.alias("s1")
    val afterStructuresAlias = Structures.alias("s2")

    override fun read(row: ResultRow): Pattern {
        val beforeJson = row[beforeStructuresAlias[Structures.json]]
        val afterJson = row[afterStructuresAlias[Structures.json]]
        return Pattern(
                beforeNode = mapper.readValue(beforeJson),
                afterNode = mapper.readValue(afterJson),
                beforeHash = row[beforeHash].toBytes(),
                afterHash = row[afterHash].toBytes()
        )
    }
}

class PatternReader(
        db: Database,
        private val nodeTypeSetId: Int? = null
) : TableReader<Pattern> by BufferedTableReader(db, Patterns, bufferSize = 1000) {
    override fun read(): Sequence<Pattern> = read {
        val query = Patterns
                .innerJoin(Patterns.beforeStructuresAlias, { beforeHash }, { Patterns.beforeStructuresAlias[Structures.hash] })
                .innerJoin(Patterns.afterStructuresAlias, { Patterns.afterHash }, { Patterns.afterStructuresAlias[Structures.hash] })
                .slice(
                        Patterns.beforeHash,                                    // beforeHash
                        Patterns.afterHash,                                     // afterHash
                        Patterns.beforeStructuresAlias[Structures.json],        // s1.json
                        Patterns.afterStructuresAlias[Structures.json],         // s2.json
                        Patterns.beforeStructuresAlias[Structures.nodeTypeSet], // s1.nodeTypeSet
                        Patterns.afterStructuresAlias[Structures.nodeTypeSet]   // s2.nodeTypeSet
                )
        /*
        SELECT
            beforeHash,
            afterHash,
            s1.json,
            s2.json,
            s1.nodeTypeSet,
            s2.nodeTypeSet
        FROM patterns
	    INNER JOIN structures AS s1
		    ON patterns.beforeHash = s1.hash
	    INNER JOIN structures AS s2
		    ON patterns.afterHash = s2.hash;
        */


        if (nodeTypeSetId != null)
            query.select {
                (Patterns.beforeStructuresAlias[Structures.nodeTypeSet] eq nodeTypeSetId) and
                        (Patterns.afterStructuresAlias[Structures.nodeTypeSet] eq nodeTypeSetId)
            }
        else
            query.selectAll()
    }
}