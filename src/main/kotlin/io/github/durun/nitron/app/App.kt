package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.durun.nitron.util.Log
import io.github.durun.nitron.util.LogLevel

class App : CliktCommand() {
    override fun run() {
        return
    }
}

fun main(args: Array<String>) {
    LogLevel = Log.Level.DEBUG
    App().subcommands(
        AstPrintCommand(),
        CodeNormalizeCommand(),
        AstJsonImportCommand(),
        PreParseCommand()
    ).main(args)
}