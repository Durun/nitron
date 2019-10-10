package io.github.durun.nitron.tester

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

inline fun <reified V> reserializeJson(value: V): Pair<String?, String?> {
    val mapper = jacksonObjectMapper()
    val json = mapper.writeValueAsString(value)
    val deserializedValue = mapper.readValue<V>(json)
    val reJson = mapper.writeValueAsString(deserializedValue)
    return Pair(json, reJson)
}