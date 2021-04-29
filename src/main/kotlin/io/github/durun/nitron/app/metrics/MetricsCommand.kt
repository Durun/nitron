package io.github.durun.nitron.app.metrics

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.util.logger
import java.nio.file.Path

class MetricsCommand : CliktCommand(name = "metrics") {
    private val outDbFiles: Path by option("-o", help = "Output Database file")
        .path()
        .required()
    private val dbFiles: List<Path> by argument(name = "DATABASE", help = "Database file")
        .path(readable = true)
        .multiple()

    private val log by logger()

    override fun run() {
        TODO("Not yet implemented")
    }
}