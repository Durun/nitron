package io.github.durun.nitron.core.config.loader

import io.github.durun.nitron.core.config.NitronConfig

object NitronConfigLoader : ConfigLoader<NitronConfig> by KSerializationConfigLoader(NitronConfig.serializer())