group = "com.github.durun.nitron"
version = "v0.6"

buildscript {
    val kotlinVersion = "1.5.21"
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
    configurations.classpath.get().resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
    }
}

plugins {
    val kotlinVersion = "1.5.21"
    `maven-publish`

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    application

    id("org.jetbrains.dokka") version "1.5.0"

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
    val kotestVersion = "4.6.1"
    val cliktVersion = "2.8.0"
    val sqliteJdbcVersion = "3.36.0.1"
    val exposedVersion = "0.17.13"
    val kotlinSerializationVersion = "1.2.2"
    val jgitVersion = "5.12.0.202106070339-r"
    val kotlinCoroutineVersion = "1.5.1-native-mt"

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
    api(kotlin("stdlib-jdk8"))

    //// Dependency for parsing
    // Java
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.26.0")

    // Use the Kotlin test library.
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}

application {
    // Define the main class for the application
    mainClassName = "com.github.durun.nitron.app.AppKt"
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
                groupId = "com.github.durun"
                artifactId = "nitron"
                version = project.version.toString()
            }
        }
    }
}
