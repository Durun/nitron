package com.github.durun.nitron.core.config.loader

import com.github.durun.nitron.core.config.ConfigWithDir
import com.github.durun.nitron.core.config.setPath
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Path

class KSerializationConfigLoader<C : ConfigWithDir>(
        private val serializer: KSerializer<C>
) : ConfigLoader<C> {
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    override fun load(jsonFile: Path): C {
        val jsonString = jsonFile.toFile().readText()
        val config = json.decodeFromString(serializer, jsonString)
        return config.setPath(jsonFile)
    }
}