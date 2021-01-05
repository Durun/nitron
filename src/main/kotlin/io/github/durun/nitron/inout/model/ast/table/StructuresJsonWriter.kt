package io.github.durun.nitron.inout.model.ast.table

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.encodeByteArray
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.ast.Structure
import io.github.durun.nitron.inout.model.ast.toSerializable
import io.github.durun.nitron.inout.model.table.writer.TableWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.File
import java.io.Flushable
import java.io.PrintWriter

class StructuresJsonWriter(
        private val out: PrintWriter,
        nodeTypePool: NodeTypePool,
        private val autoFlush: Boolean = true
) : TableWriter<Structure>, Flushable, Closeable {
    companion object {
        private val mapper = jacksonObjectMapper()
    }

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
        val json = Json.encodeToString(value)
        out.println(json)
        if (autoFlush) flush()
    }

    private fun writeAsString(value: Structure): String {
        val hash = encodeByteArray(value.hash)
        val ast = mapper.writeValueAsString(value.ast)
        return """{"$hash":$ast}"""
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