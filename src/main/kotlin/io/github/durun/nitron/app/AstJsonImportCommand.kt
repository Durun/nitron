package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import io.github.durun.nitron.inout.model.ast.structure.simple.AstJsonReader
import io.github.durun.nitron.inout.model.ast.structure.simple.AstTableWriter
import java.io.File
import java.nio.file.Path

class AstJsonImportCommand : CliktCommand(
        name = "importAst"
) {
    private val input: File by argument(name = "input", help = "input json file")
            .file(readable = true)

    private val output: File by argument(name = "output", help = "output Database")
            .file(writable = true)

    override fun run() {
        AstJsonImporter(input = input, output = output)
                .run()
    }
}


private class AstJsonImporter(
        private val reader: AstJsonReader,
        private val writer: AstTableWriter
) {
    constructor(input: Path, output: Path) : this(
            reader = AstJsonReader(input, bufferSize = 100000, progressOutput = System.out),
            writer = AstTableWriter(output, bufferSize = 1000)
    )

    constructor(input: File, output: File) : this(input.toPath(), output.toPath())

    fun run() {
        val typeSet = reader.nodeTypePool
        val nodes = reader.readHashAndRawNodes()

        writer.writeBuffering(typeSet, nodes)
    }
}