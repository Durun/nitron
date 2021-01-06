package io.github.durun.nitron.inout.model.ast.structure.simple

import io.github.durun.nitron.core.ast.type.NodeTypePool
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class AstJsonReader(file: File, bufferSize: Int, progressOutput: PrintStream? = null) {
    constructor(path: Path, bufferSize: Int, progressOutput: PrintStream? = null) : this(path.toFile(), bufferSize, progressOutput)

    val nodeTypePool: NodeTypePool
    private val rawSequence: Sequence<String>

    private var readSize: Long = 0
    private val fileSize: Long = file.length()

    init {
        val reader = file.bufferedReader(bufferSize = bufferSize)
        nodeTypePool = Json.decodeFromString(reader.readLine())

        rawSequence =
                if (progressOutput == null) reader.lineSequence()
                else reader.lineSequence()
                        .onEach {
                            // print readSize/fileSize percentage
                            val before = 100 * readSize / fileSize
                            readSize += it.toByteArray().size
                            val after = 100 * readSize / fileSize
                            if (before != after) progressOutput.print("Progress: $after%\r")
                        }
    }
    fun readLine(): Sequence<String> = rawSequence
}