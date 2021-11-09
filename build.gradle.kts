group = "com.github.durun.nitron"
version = "v0.13"

buildscript {
    val kotlinVersion = "1.5.31"
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
    val kotlinVersion = "1.5.31"
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
    val kotlinVersion = "1.5.31"

    // This dependency is used by the application.
    implementation("org.antlr:antlr4:4.9.2")
    implementation("com.github.julianthome:inmemantlr-api:1.7.0")
    implementation("com.github.ajalt:clikt:2.8.0")
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")
    implementation("org.jetbrains.exposed:exposed:0.17.14")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("jaxen:jaxen:1.2.0")
    implementation("org.apache.commons:commons-text:1.9")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    api(kotlin("stdlib-jdk8", kotlinVersion))

    //// Dependency for parsing
    // Java
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.27.0")

    // Use the Kotlin test library.
    val kotestVersion = "4.6.3"
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
