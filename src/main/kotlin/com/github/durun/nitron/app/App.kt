package com.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.durun.nitron.app.metrics.MetricsCommand
import com.github.durun.nitron.app.preparse.FetchCommand
import com.github.durun.nitron.app.preparse.ParseCommand
import com.github.durun.nitron.app.preparse.RegisterCommand
import com.github.durun.nitron.util.Log
import com.github.durun.nitron.util.LogLevel

class App : CliktCommand() {
    override fun run() {
        return
    }
}

fun main(args: Array<String>) {
	LogLevel = Log.Level.INFO
    App().subcommands(
        AstPrintCommand(),
        CodeNormalizeCommand(),
        AstJsonImportCommand(),
        RegisterCommand(),
        FetchCommand(),
        ParseCommand(),
        MetricsCommand()
    ).main(args)
}