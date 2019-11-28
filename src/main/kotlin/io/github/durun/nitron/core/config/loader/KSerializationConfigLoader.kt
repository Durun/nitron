package io.github.durun.nitron.core.config.loader

import io.github.durun.nitron.core.config.ConfigWithDir
import io.github.durun.nitron.core.config.setPath
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Path

class KSerializationConfigLoader<C : ConfigWithDir>(
        private val serializer: KSerializer<C>
) : ConfigLoader<C> {
    override fun load(jsonFile: Path): C {
        val json = jsonFile
                .toFile()
                .bufferedReader()
                .readText()
        val config = Json.parse(serializer, json)
        return config.setPath(jsonFile)
    }
}