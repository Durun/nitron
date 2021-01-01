package io.github.durun.nitron.validation

class TemporaryTest(private val testBody: TemporaryTest.() -> Unit) {
	fun execute(): Unit = testBody()

	fun addTestCase(name: String, test: () -> Unit) {
		print("$name: ")
		runCatching { test() }
				.onSuccess { println("OK") }
				.onFailure {
					println("Failed: ${it.message}")
					it.printStackTrace(System.err)
				}
	}

	infix operator fun String.invoke(test: () -> Unit) = addTestCase(this, test)

	fun addTestCaseDir(name: String, test: TemporaryTest.() -> Unit) {
		println("[$name]")
		runCatching { test() }
				.onFailure { it.printStackTrace(System.err) }
	}

	infix operator fun String.minus(test: TemporaryTest.() -> Unit) = addTestCaseDir(this, test)
}