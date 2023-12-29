plugins {
    kotlin("jvm") version "1.9.21"
    `java-library`
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.jetbrains.dokka") version "1.8.10"
    id("signing")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

apply(plugin = "io.github.gradle-nexus.publish-plugin")

group = "io.exoquery"
version = "1.0"

apply(plugin = "kotlin")
apply(plugin = "maven-publish")

repositories {
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    implementation("com.lihaoyi:fansi_2.13:0.4.0")
    implementation("com.lihaoyi:pprint_2.13:0.8.1")
    implementation(kotlin("reflect"))
}

//tasks.test {
//    useJUnitPlatform()
//}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}