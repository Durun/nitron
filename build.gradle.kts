group = "io.github.durun.nitron"
version = "0.1"

plugins {
    `maven-publish`

    kotlin("jvm") version "1.5.0"
    kotlin("plugin.serialization") version "1.4.32"

    application

    id("org.jetbrains.dokka") version "1.4.30"

    // for making fatJar
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

repositories {
    jcenter()
}

dependencies {
    // Versions
    val antlrVersion = "4.9.2"
    val inmemantlrVersion = "1.7.0"
    val kotestVersion = "4.4.3"
    val cliktVersion = "2.4.0"
    val sqliteJdbcVersion = "3.34.0"
    val exposedVersion = "0.17.13"
    val kotlinSerializationVersion = "1.0.1"
    val jgitVersion = "5.11.0.202103091610-r"
    val kotlinCoroutineVersion = "1.4.3-native-mt"

    // This dependency is used by the application.
    implementation("org.antlr:antlr4:$antlrVersion")
    implementation("com.github.julianthome:inmemantlr-api:$inmemantlrVersion")
    implementation("com.github.ajalt:clikt:$cliktVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
    implementation("org.jetbrains.exposed:exposed:$exposedVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutineVersion")
    implementation("jaxen:jaxen:1.2.0")
    implementation("org.apache.commons:commons-text:1.9")

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
 * Maven Publishing
 */
tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }
}
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["kotlin"])
                artifact(tasks["sourcesJar"])
                groupId = "io.github.durun"
                artifactId = "nitron"
                version = project.version.toString()
            }
        }
    }
}