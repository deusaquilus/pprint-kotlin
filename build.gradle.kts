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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
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
        publications.withType<MavenPublication> {
            pom {
                name.set("decomat")
                description.set("DecoMat - Deconstructive Pattern Matching for Kotlin")
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
                    connection.set("scm:git:git://github.com/ExoQuery/DecoMat.git")
                    developerConnection.set("scm:git:ssh://github.com:ExoQuery/DecoMat.git")
                }
            }
        }
    }

    signing {
        sign(publishing.publications)
    }

    tasks.withType<Sign> {
        onlyIf { !project.hasProperty("nosign") }
    }
}