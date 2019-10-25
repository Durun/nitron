package io.github.durun.nitron.core.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Path

class KSerializationConfigLoader<C>(
        private val serializer: KSerializer<C>
) : ConfigLoader<C> {
    override fun load(jsonFile: Path): C {
        val json = jsonFile
                .toFile()
                .bufferedReader()
                .readText()
        return Json.parse(serializer, json)
    }
}