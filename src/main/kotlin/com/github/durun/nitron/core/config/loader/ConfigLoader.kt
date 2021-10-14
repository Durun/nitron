package com.github.durun.nitron.core.config.loader

import java.nio.file.Path

interface ConfigLoader<C> {
    fun load(jsonPath: Path): C
}