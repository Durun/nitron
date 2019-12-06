package io.github.durun.nitron.inout.model.ast.structure.simple

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.core.decodeByteArray
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class AstJsonReader(file: File, bufferSize: Int, progressOutput: PrintStream? = null) {
    constructor(path: Path, bufferSize: Int, progressOutput: PrintStream? = null) : this(path.toFile(), bufferSize, progressOutput)

    private val mapper = jacksonObjectMapper()
    private val nodeTypeSet: NodeTypeSet
    private val rawSequence: Sequence<String>
    private val hashAndNodeSequence: Sequence<Pair<ByteArray, String>>

    private var readSize: Long = 0
    private val fileSize: Long = file.length()

    init {
        val reader = file.bufferedReader(bufferSize = bufferSize)
        nodeTypeSet = mapper.readValue(reader.readLine())

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

        hashAndNodeSequence = rawSequence
                .map {
                    val hashString = it                 // {"[1,2, ... ,16]":{...}}
                            .substringBefore(delimiter = ':')   // {"[1,2, ... ,16]"
                            .drop(n = 2)                        // [1,2, ... ,16]"
                            .dropLast(n = 1)                    // [1,2, ... ,16]
                    val nodeString = it
                            .substringAfter(delimiter = ':')    // {...}}
                            .dropLast(n = 1)                    // {...}
                    decodeByteArray(hashString) to nodeString
                }
    }


    fun readTypeSet(): NodeTypeSet = nodeTypeSet
    fun readHashAndRawNodes(): Sequence<Pair<ByteArray, String>> = hashAndNodeSequence
}