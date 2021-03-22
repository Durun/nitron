package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.app.main
import io.kotest.core.spec.style.FreeSpec
import kotlin.io.path.createTempDirectory

private fun execCommand(line: String) = main(line.split(" ").toTypedArray())
	.also { println("Executed $line") }

@kotlin.io.path.ExperimentalPathApi
class ParseCommandTest : FreeSpec({
	"example" {
		val dir = createTempDirectory("cache")
			.apply { toFile().deleteOnExit() }
		println("Created temp directory: $dir")

		val db = dir.resolve("cache.db")
		println("Created temp database: $db")

		execCommand("preparse-register --remote https://github.com/githubtraining/hellogitworld --lang java $db")

		execCommand("preparse-fetch --branch master --dir $dir $db")

		execCommand("preparse --repository https://github.com/githubtraining/hellogitworld --dir $dir $db")
	}
})