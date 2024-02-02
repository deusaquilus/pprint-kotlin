import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `maven-publish`
    signing
    kotlin("multiplatform") version "1.9.22" apply false
    id("io.kotest.multiplatform") version "5.8.0" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.jetbrains.dokka") version "1.8.10"
}

allprojects {
    repositories {
        // mavenLocal() including this first causes all sorts of horror
        // with gradle not seeing classes/dependencies that should be there
        mavenCentral()
    }
}

allprojects {
    group = "io.exoquery"
    version = "2.0.0"
}

subprojects {
    //val varintName = project.name

    apply {
        plugin("org.jetbrains.dokka")
        plugin("maven-publish")
        plugin("signing")
    }

    publishing {
        val user = System.getenv("SONATYPE_USERNAME")
        val pass = System.getenv("SONATYPE_PASSWORD")

        repositories {
            maven {
                name = "Oss"
                setUrl {
                    val repositoryId = System.getenv("SONATYPE_REPOSITORY_ID") ?: error("Missing env variable: SONATYPE_REPOSITORY_ID")
                    "https://s01.oss.sonatype.org/service/local/staging/deployByRepositoryId/$repositoryId/"
                }
                credentials {
                    username = user
                    password = pass
                }
            }
            maven {
                name = "Snapshot"
                setUrl { "https://s01.oss.sonatype.org/content/repositories/snapshots/" }
                credentials {
                    username = user
                    password = pass
                }
            }
        }

        publications.withType<MavenPublication> {
            pom {
                name.set("pprint-kotlin")
                description.set("Pretty Printing for Kotlin")
                url.set("https://github.com/deusaquilus/pprint-kotlin")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        name.set("Alexander Ioffe")
                        email.set("deusaquilus@gmail.com")
                        organization.set("github")
                        organizationUrl.set("http://www.github.com")
                    }
                }

                scm {
                    url.set("https://github.com/exoquery/decomat/tree/main")
                    connection.set("scm:git:git://github.com/ExoQuery/pprint-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com:ExoQuery/pprint-kotlin.git")
                }
            }
        }
    }

    signing {
        useInMemoryPgpKeys(
            System.getenv("GPG_PRIVATE_KEY").chunked(64).joinToString("\n"),
            System.getenv("GPG_PRIVATE_PASSWORD")
        )
        sign(publishing.publications)
    }

    tasks.withType<Sign> {
        onlyIf { !project.hasProperty("nosign") }
    }
}