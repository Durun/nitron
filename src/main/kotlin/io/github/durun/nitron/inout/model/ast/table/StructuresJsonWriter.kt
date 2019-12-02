package io.github.durun.nitron.inout.model.ast.table

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.durun.nitron.core.encodeByteArray
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.table.writer.TableWriter
import java.io.Closeable
import java.io.File
import java.io.Flushable
import java.io.PrintWriter

class StructuresJsonWriter(
        private val out: PrintWriter,
        nodeTypeSet: NodeTypeSet,
        private val autoFlush: Boolean = true
) : TableWriter<Structure>, Flushable, Closeable {
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

    override fun flush() {
        out.flush()
    }

    override fun close() {
        flush()
        out.close()
    }

    private fun write(value: NodeTypeSet) {
        val json = mapper.writeValueAsString(value)
        out.println(json)
        if (autoFlush) flush()
    }

    private fun writeAsString(value: Structure): String {
        val hash = encodeByteArray(value.hash)
        val ast = mapper.writeValueAsString(value.ast)
        return "{$hash:$ast}"
    }

    override fun write(value: Structure) {
        val json = writeAsString(value)
        out.println(json)
        if (autoFlush) flush()
    }

    override fun write(values: List<Structure>) {
        values.forEach {
            val json = writeAsString(it)
            out.println(json)
        }
        if (autoFlush) flush()
    }

    override fun write(values: Sequence<Structure>) {
        values.forEach {
            val json = writeAsString(it)
            out.println(json)
        }
        if (autoFlush) flush()
    }
}