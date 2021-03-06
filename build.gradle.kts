group = "io.github.durun.nitron"
version = "0.2-SNAPSHOT"

plugins {
    `maven-publish`

    kotlin("jvm") version "1.5.20"
    kotlin("plugin.serialization") version "1.5.20"

    application

    id("org.jetbrains.dokka") version "1.4.32"

    // for making fatJar
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    // Versions
    val antlrVersion = "4.9.2"
    val inmemantlrVersion = "1.7.0"
    val kotestVersion = "4.6.0"
    val cliktVersion = "2.8.0"
    val sqliteJdbcVersion = "3.36.0"
    val exposedVersion = "0.17.13"
    val kotlinSerializationVersion = "1.2.1"
    val jgitVersion = "5.12.0.202106070339-r"
    val kotlinCoroutineVersion = "1.5.0-native-mt"

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
    implementation(kotlin("stdlib-jdk8"))

    //// Dependency for parsing
    // Java
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.26.0")

    // Use the Kotlin test library.
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}

application {
    // Define the main class for the application
    mainClassName = "io.github.durun.nitron.app.AppKt"
    mainClass.set(mainClassName)
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
