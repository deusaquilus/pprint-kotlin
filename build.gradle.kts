plugins {
    kotlin("jvm") version "1.9.21"
    `java-library`
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.jetbrains.dokka") version "1.9.10"
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
version = "1.0.0"

apply(plugin = "kotlin")
apply(plugin = "maven-publish")

repositories {
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    implementation(kotlin("reflect"))
}

// Needed for Kotest
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

allprojects {

    val varintName = project.name

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.dokka")
    }

    val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

    tasks {
        val javadocJar by creating(Jar::class) {
            dependsOn(dokkaHtml)
            archiveClassifier.set("javadoc")
            from(dokkaHtml.outputDirectory)
        }
        val sourcesJar by creating(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets["main"].allSource)
        }
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["kotlin"])
                artifactId = varintName

                artifact(tasks["javadocJar"])
                artifact(tasks["sourcesJar"])

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
    }

    // Check the 'skipSigning' project property
    if (!project.hasProperty("nosign")) {
        // If 'skipSigning' is not present, apply the signing plugin and configure it
        apply(plugin = "signing")

        signing {
            // use the properties passed as command line args
            // -Psigning.keyId=${{secrets.SIGNING_KEY_ID}} -Psigning.password=${{secrets.SIGNING_PASSWORD}} -Psigning.secretKeyRingFile=$(echo ~/.gradle/secring.gpg)
            sign(publishing.publications["mavenJava"])
        }
    }
}