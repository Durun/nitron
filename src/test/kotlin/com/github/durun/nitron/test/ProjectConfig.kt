package com.github.durun.nitron.test

import io.kotest.core.config.AbstractProjectConfig

object ProjectConfig : AbstractProjectConfig() {
	override val parallelism = 8
}