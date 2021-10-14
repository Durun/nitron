package com.github.durun.nitron.core.config.loader

import com.github.durun.nitron.core.config.ConfigWithDir
import com.github.durun.nitron.core.config.setPath
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText

class KSerializationConfigLoader<C : ConfigWithDir>(
        private val serializer: KSerializer<C>
) : ConfigLoader<C> {
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    override fun load(jsonPath: Path): C {
        val jsonString = jsonPath.readText()
        val config = json.decodeFromString(serializer, jsonString)
        return config.setPath(jsonPath)
    }
}