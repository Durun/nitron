package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.ast.type.AstSerializers
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.ast.structure.simple.AstTableWriter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.sql.Database
import java.io.BufferedReader
import java.io.File
import kotlin.io.path.ExperimentalPathApi

class AstJsonImportCommand : CliktCommand(
        name = "importAst"
) {
    private val input: File by argument(name = "input", help = "input json file")
            .file(readable = true)

    private val output: File by argument(name = "output", help = "output Database")
            .file(writable = true)

    @ExperimentalPathApi
    override fun run() {
        SQLiteDatabase.connect(output.toPath()).writeAstJson(
                input = input.bufferedReader(),
                fileSize = output.length()
        )
    }
}

fun Database.writeAstJson(
        input: BufferedReader,
        fileSize: Long? = null
) {
    val typeSet: NodeTypePool = Json.decodeFromString(input.readLine())
    val decoder = AstSerializers.json(typeSet)

    var readSize: Long = 0
    val nodes = input.lineSequence()
            .onEach {
                if (fileSize != null) {
                    // print readSize/fileSize percentage
                    val before = 100 * readSize / fileSize
                    readSize += it.toByteArray().size
                    val after = 100 * readSize / fileSize
                    if (before != after) print("Progress: $after%\r")
                }
            }
            .map {
                val obj = Json.parseToJsonElement(it).jsonObject
                val hash: MD5 = decoder.decodeFromJsonElement(obj["hash"]!!)
                val asts = obj["asts"]!!.toString()
                hash to asts
            }
    AstTableWriter(this).writeBuffering(typeSet, nodes)
}