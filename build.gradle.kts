/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.50"

    // Apply the application plugin to add support for building a CLI application.
    application

    id("org.ajoberstar.grgit") version "4.0.0-rc.1"
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // Versions
    val jacksonVersion = "2.10.0"
    val antlrVersion = "4.7.2"
    val inmemantlrVersion = "1.6"
    val kotlintestVersion = "3.4.2"

    // This dependency is used by the application.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.antlr:antlr4:$antlrVersion")
    implementation("com.github.julianthome:inmemantlr-api:$inmemantlrVersion")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintestVersion")
}

application {
    // Define the main class for the application
    mainClassName = "io.github.durun.nitron.AppKt"
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

fun updateGitRepo(
        path: String,
        gitUrl: String,
        branch: String = "master")
{
    val repo = kotlin.runCatching {
        org.ajoberstar.grgit.Grgit.open(mapOf("dir" to path))
    }.getOrElse {
        org.ajoberstar.grgit.Grgit.clone(mapOf("dir" to path, "uri" to gitUrl))
    }
    repo.pull(mapOf("branch" to branch))
}

tasks.register("updateTestGrammar") {
    val testDataDir = "${project.projectDir}/testdata"
    val testGrammarDir = "${testDataDir}/grammars"
    val testGrammarUrl = "https://github.com/antlr/grammars-v4.git"
    updateGitRepo(
            path=testGrammarDir,
            gitUrl=testGrammarUrl
    )
}
test.dependsOn(tasks["updateTestGrammar"])
