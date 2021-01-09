
group = "io.github.durun"
version = "0.1-SNAPSHOT"

plugins {
    `maven-publish`

    kotlin("jvm") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.21"

    application

    id("org.jetbrains.dokka") version "0.10.0"

    // for making fatJar
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

repositories {
    jcenter()
}

dependencies {
    // Versions
    val antlrVersion = "4.9"
    val inmemantlrVersion = "1.7.0"
    val kotestVersion = "4.3.2"
    val cliktVersion = "2.4.0"
    val sqliteJdbcVersion = "3.34.0"
    val exposedVersion = "0.17.7"
    val kotlinSerializationVersion = "1.0.1"

    // This dependency is used by the application.
    implementation("org.antlr:antlr4:$antlrVersion")
    implementation("com.github.julianthome:inmemantlr-api:$inmemantlrVersion")
    implementation("com.github.ajalt:clikt:$cliktVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
    implementation("org.jetbrains.exposed:exposed:$exposedVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}

application {
    // Define the main class for the application
    mainClass.set("io.github.durun.nitron.app.AppKt")
    mainClassName = "io.github.durun.nitron.app.AppKt"
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    test {
        useJUnitPlatform()
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


/**
 * Generate KDoc
 */
tasks {
    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

/**
 * Maven Publishing
 */
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
tasks {
    build {
        dependsOn(publish)
    }
}
