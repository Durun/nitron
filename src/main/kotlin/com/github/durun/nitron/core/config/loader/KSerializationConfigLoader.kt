package com.github.durun.nitron.core.config.loader

import com.github.durun.nitron.core.config.ConfigWithDir
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.Reader
import java.net.URI

class KSerializationConfigLoader<C : ConfigWithDir>(
        private val serializer: KSerializer<C>
) : ConfigLoader<C> {
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    override fun load(uri: URI, reader: Reader): C {
        val jsonString = reader.readText()
        val config = json.decodeFromString(serializer, jsonString)
        config.uri = uri
        return config
    }
}