import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.dokka.gradle.DokkaTask

group = "io.github.durun"
version = "0.1-SNAPSHOT"

val kotlinVersion = plugins.getPlugin(KotlinPluginWrapper::class.java).kotlinPluginVersion

plugins {
    `maven-publish`
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"

    // Apply the application plugin to add support for building a CLI application.
    application

    id("org.jetbrains.dokka") version "0.10.0"

    // for making fatJar
    id("com.github.johnrengelman.shadow") version "5.2.0"
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
    val kotlinSerializationVersion = "1.0.0"

    // This dependency is used by the application.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.antlr:antlr4:$antlrVersion")
    implementation("com.github.julianthome:inmemantlr-api:$inmemantlrVersion")
    implementation("com.github.ajalt:clikt:$cliktVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
    compile("org.jetbrains.exposed:exposed:$exposedVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

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

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val build = tasks["build"]
val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}


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
