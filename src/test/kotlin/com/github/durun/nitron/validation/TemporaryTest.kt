package com.github.durun.nitron.validation

import java.io.PrintStream

class TemporaryTest(
    private val output: PrintStream = System.out,
    private val testBody: TemporaryTest.() -> Unit
) {
    fun execute(): Unit = testBody()

    private val ignorePrefix = "!"

    fun addTestCase(name: String, ignore: Boolean = false, test: () -> Unit) {
        if (name.startsWith(ignorePrefix)) return addTestCase(
            name.drop(ignorePrefix.length),
            ignore = true,
            test = test
        )
        output.println("### $name")
        if (ignore) {
            output.println("Ignored")
            return
		}
        runCatching { test() }
            .onSuccess { output.println("OK") }
				.onFailure {
                    output.println("Failed: ${it.message}")
                    output.println("```")
                    it.printStackTrace(output)
                    output.println("```")
                }
	}

	infix operator fun String.invoke(test: () -> Unit) = addTestCase(this, test = test)

	fun addTestCaseDir(name: String, ignore: Boolean = false, test: TemporaryTest.() -> Unit) {
		if (name.startsWith(ignorePrefix)) return addTestCaseDir(name.drop(ignorePrefix.length), ignore = true, test = test)
		if (ignore) {
            output.println("## [$name]\n Ignored")
            return
        } else {
            output.println("## [$name]")
        }
        runCatching { test() }
            .onFailure { it.printStackTrace(output) }
    }

    infix operator fun String.minus(test: TemporaryTest.() -> Unit) = addTestCaseDir(this, test = test)
}

fun testReportAsMarkDown(testBody: TemporaryTest.() -> Unit) {
    val file = kotlin.io.path.createTempFile(suffix = ".md")
    println("Test start.")
    TemporaryTest(output = PrintStream(file.toFile()), testBody = testBody)
        .execute()
    println("Test finished: ${file.toUri()}")
}