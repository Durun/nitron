package com.github.durun.nitron.core.config.loader

import java.io.Reader
import java.net.URI
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.bufferedReader

interface ConfigLoader<C> {
    fun load(path: Path): C {
        return load(path.toUri(), path.bufferedReader())
    }

    fun load(uri: URI): C {
        return uri.toURL().openStream().use {
            load(uri, it.bufferedReader())
        }
    }

    fun load(url: URL): C {
        return url.openStream().use {
            load(url.toURI(), it.bufferedReader())
        }
    }

    fun load(uri: URI, reader: Reader): C
}