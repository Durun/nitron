package io.github.durun.nitron.core.config.loader

import io.github.durun.nitron.core.config.LangConfig

object LangConfigLoader : ConfigLoader<LangConfig> by KSerializationConfigLoader(LangConfig.serializer())