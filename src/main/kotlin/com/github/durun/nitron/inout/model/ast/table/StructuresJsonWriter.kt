package com.github.durun.nitron.inout.model.ast.table

import com.github.durun.nitron.core.AstSerializers
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.inout.model.ast.Structure
import com.github.durun.nitron.inout.model.table.writer.TableWriter
import kotlinx.serialization.encodeToString
import java.io.Closeable
import java.io.File
import java.io.Flushable
import java.io.PrintWriter

class StructuresJsonWriter(
        private val out: PrintWriter,
        nodeTypePool: NodeTypePool,
        private val autoFlush: Boolean = true
) : TableWriter<Structure>, Flushable, Closeable {
    constructor(file: File, nodeTypePool: NodeTypePool) : this(
            out = file.printWriter(),
            nodeTypePool = nodeTypePool
    )

    init {
        // Write header
        write(nodeTypePool)
    }

    override fun flush() {
        out.flush()
    }

    override fun close() {
        flush()
        out.close()
    }

    private fun write(value: NodeTypePool) {
        val json = AstSerializers.encodeOnlyJson.encodeToString(value)
        out.println(json)
        if (autoFlush) flush()
    }

    private fun writeAsString(value: Structure): String {
        return AstSerializers.encodeOnlyJson.encodeToString(value)
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