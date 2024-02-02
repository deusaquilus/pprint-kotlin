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
        val signingKeyRaw = System.getenv("NEW_SIGNING_KEY_ID_BASE64")
        if (signingKeyRaw == null) error("ERROR: No Signing Key Found")
        // Seems like the right way was to have newlines after all the exported (ascii armored) lines
        // and you can put them into the github-var with newlines but if you
        // include the "-----BEGIN PGP PRIVATE KEY BLOCK-----" and "-----END PGP PRIVATE KEY BLOCK-----"
        // parts with that then errors happen. Have a look at https://github.com/gradle/gradle/issues/15718 for more detail
        // Ultimately however `iurysza` is only partially correct and they key-itself does not need to be escaped
        // and can be put into a github-var with newlines.
        val signingKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n\n${signingKeyRaw}\n-----END PGP PRIVATE KEY BLOCK-----"
        useInMemoryPgpKeys(
            System.getenv("NEW_SIGNING_KEY_ID_BASE64_ID"),
            signingKey,
            System.getenv("NEW_SIGNING_KEY_ID_BASE64_PASS")
        )
        sign(publishing.publications)
    }

    tasks.withType<Sign> {
        onlyIf { !project.hasProperty("nosign") }
    }

    // Fix for Kotlin issue: https://youtrack.jetbrains.com/issue/KT-61313
    tasks.withType<Sign>().configureEach {
        val pubName = name.removePrefix("sign").removeSuffix("Publication")

        // These tasks only exist for native targets, hence findByName() to avoid trying to find them for other targets

        // Task ':linkDebugTest<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
        tasks.findByName("linkDebugTest$pubName")?.let {
            mustRunAfter(it)
        }
        // Task ':compileTestKotlin<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
        tasks.findByName("compileTestKotlin$pubName")?.let {
            mustRunAfter(it)
        }
    }
}