package io.github.durun.nitron.core.config

object LangConfigLoader : ConfigLoader<LangConfig> by KSerializationConfigLoader(LangConfig.serializer())