package com.github.durun.nitron.core.config.loader

import com.github.durun.nitron.core.config.LangConfig

object LangConfigLoader : ConfigLoader<LangConfig> by KSerializationConfigLoader(LangConfig.serializer())