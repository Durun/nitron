package io.github.durun.nitron.validation

class TemporaryTest(private val testBody: TemporaryTest.() -> Unit) {
	fun execute(): Unit = testBody()

	private val ignorePrefix = "!"

	fun addTestCase(name: String, ignore: Boolean = false, test: () -> Unit) {
		if (name.startsWith(ignorePrefix)) return addTestCase(name.drop(ignorePrefix.length), ignore = true, test = test)
		print("$name: ")
		if (ignore) {
			println("Ignored")
			return
		}
		runCatching { test() }
				.onSuccess { println("OK") }
				.onFailure {
					println("Failed: ${it.message}")
					it.printStackTrace(System.err)
				}
	}

	infix operator fun String.invoke(test: () -> Unit) = addTestCase(this, test = test)

	fun addTestCaseDir(name: String, ignore: Boolean = false, test: TemporaryTest.() -> Unit) {
		if (name.startsWith(ignorePrefix)) return addTestCaseDir(name.drop(ignorePrefix.length), ignore = true, test = test)
		if (ignore) {
			println("[$name]: Ignored")
			return
		} else {
			println("[$name]")
		}
		runCatching { test() }
				.onFailure { it.printStackTrace(System.err) }
	}

	infix operator fun String.minus(test: TemporaryTest.() -> Unit) = addTestCaseDir(this, test = test)
}