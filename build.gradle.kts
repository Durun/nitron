group = "com.github.durun.nitron"
version = "v0.18"

buildscript {
    val kotlinVersion: String by extra("1.6.20")

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
    val kotlinVersion = "1.6.20"

    `maven-publish`

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    application

    id("org.jetbrains.dokka") version "1.7.10"

    // for making fatJar
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlinVersion: String by project

    // This dependency is used by the application.
    implementation("org.antlr:antlr4:4.9.3")
    implementation("com.github.julianthome:inmemantlr-api:1.9.2")
    implementation("com.github.ajalt.clikt:clikt:3.4.1")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("org.jetbrains.exposed:exposed:0.17.14")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1-native-mt")
    implementation("jaxen:jaxen:1.2.0")
    implementation("org.apache.commons:commons-text:1.9")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    api(kotlin("stdlib-jdk8", kotlinVersion))

    //// Dependency for parsing
    // Java
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.29.0")

    // Use the Kotlin test library.
    val kotestVersion = "5.3.0"
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
