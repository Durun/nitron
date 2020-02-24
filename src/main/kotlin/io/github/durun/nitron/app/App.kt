package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class App : CliktCommand() {
	override fun run() {
		return
	}
}

fun main(args: Array<String>) = App().subcommands(
		AstPrintCommand(),
		CodeNormalizeCommand(),
		AstJsonImportCommand(),
		CasestudyCommand()
).main(args)