package com.github.durun.nitron.util

import java.net.URI

fun URI.resolveInJar(str: String): URI {
    return when (this.scheme) {
        "jar" -> {
            val filePart = URI.create(this.schemeSpecificPart).resolve(str)
            URI("jar:$filePart")
        }
        else -> this.resolve(str)
    }
}