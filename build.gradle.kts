/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask

group = "io.github.durun"
version = "0.1-SNAPSHOT"

plugins {
    `maven-publish`
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.50"

    // Apply the application plugin to add support for building a CLI application.
    application

    id("org.ajoberstar.grgit") version "4.0.0-rc.1"
    id("org.jetbrains.dokka") version "0.10.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.50"
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
    val cliktVersion = "2.2.0"
    val sqliteJdbcVersion = "3.28.0"
    val exposedVersion = "0.17.6"
    val kotlinSerializationVersion = "0.13.0"

    // This dependency is used by the application.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.antlr:antlr4:$antlrVersion")
    implementation("com.github.julianthome:inmemantlr-api:$inmemantlrVersion")
    implementation("com.github.ajalt:clikt:$cliktVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
    implementation("org.jetbrains.exposed:exposed:$exposedVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintestVersion")
}

application {
    // Define the main class for the application
    mainClassName = "io.github.durun.nitron.app.AppKt"
}

val build = tasks["build"]
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

val updateTestGrammar by tasks.register("updateTestGrammar") {
    val testDataDir = "${project.projectDir}/testdata"
    val testGrammarDir = "${testDataDir}/grammars"
    val testGrammarUrl = "https://github.com/antlr/grammars-v4.git"
    updateGitRepo(
            path=testGrammarDir,
            gitUrl=testGrammarUrl
    )
}
test.dependsOn(updateTestGrammar)


/**
 * Creates fat-jar/uber-jar.
 */
val fatJar by tasks.register<Jar>("fatJar") {
    baseName = "${project.name}-fatJar"
    manifest {
        attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Main-Class" to project.application.mainClassName
        )
    }
    from(
            configurations.runtimeClasspath.get().map{
                if (it.isDirectory) it else zipTree(it)
            }
    )
    with(tasks.jar.get() as CopySpec)
}
build.dependsOn(fatJar)

/**
 * Generate KDoc
 */
tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    from(tasks.dokka)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(dokkaJar)
        }
    }
    repositories {
        maven {
            url = uri("$buildDir/repository")
        }
    }
}
val publish = tasks["publish"]
build.dependsOn(publish)