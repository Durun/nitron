package io.github.durun.nitron.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

fun getGrammarList(dir: Path): List<Path> {
    return Files.list(dir)
            .filter { it.toString().endsWith(".g4") }
            .collect(Collectors.toList())
}