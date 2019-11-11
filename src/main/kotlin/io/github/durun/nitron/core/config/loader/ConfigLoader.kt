package io.github.durun.nitron.core.config.loader

import java.nio.file.Path

interface ConfigLoader<C> {
    fun load(jsonFile: Path): C
}