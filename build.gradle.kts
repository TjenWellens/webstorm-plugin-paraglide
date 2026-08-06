import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        webstorm("2025.2.6")

        bundledPlugin("JavaScript")

        testFramework(TestFrameworkType.Platform)
    }
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(21)
}