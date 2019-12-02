package io.github.durun.nitron.inout.model.ast.table

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.durun.nitron.core.decodeByteArray
import io.github.durun.nitron.core.encodeByteArray
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.writer.TableWriter
import java.io.Closeable
import java.io.File
import java.io.PrintWriter

class StructuresJsonWriter(
        private val out: PrintWriter,
        nodeTypeSet: NodeTypeSet
) : TableWriter<Structure>, Closeable {
    companion object {
        private val mapper = jacksonObjectMapper()
    }

    constructor(file: File, nodeTypeSet: NodeTypeSet) : this(
            out = file.printWriter(),
            nodeTypeSet = nodeTypeSet
    )

    init {
        // Write header
        write(nodeTypeSet)
    }

    override fun close() {
        out.flush()
        out.close()
    }

    private fun write(value: NodeTypeSet) {
        val json = mapper.writeValueAsString(value)
        out.println(json)
    }

    override fun write(value: Structure) {
        val hash = encodeByteArray(value.hash)
        val p = decodeByteArray(hash)
        assert(p.contentEquals(value.hash))
        val ast = mapper.writeValueAsString(value.ast)
        val json = "{$hash:$ast}"
        out.println(json)
    }

    override fun write(values: List<Structure>) {
        values.forEach { write(it) }
    }

    override fun write(values: Sequence<Structure>) {
        values.forEach { write(it) }
    }
}