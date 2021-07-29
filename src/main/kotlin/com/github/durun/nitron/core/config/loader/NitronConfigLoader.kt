package com.github.durun.nitron.core.config.loader

import com.github.durun.nitron.core.config.NitronConfig

object NitronConfigLoader : ConfigLoader<NitronConfig> by KSerializationConfigLoader(NitronConfig.serializer())