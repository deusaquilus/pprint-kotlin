import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  kotlin("jvm")
  `java-library`
  signing
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  testImplementation(kotlin("test"))
  testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
  implementation(kotlin("reflect"))
  api(project(":pprint-kotlin-core"))
}

publishing {
  val varintName = project.name

  val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

  tasks {
    val sourcesJar by creating(Jar::class) {
      archiveClassifier.set("sources")
      from(sourceSets["main"].allSource)
    }
  }

  publications {
    create<MavenPublication>("mavenJava") {
      from(components["kotlin"])
      artifactId = varintName

      artifact(tasks["sourcesJar"])

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
          url.set("https://github.com/exoquery/pprint-kotlin/tree/main")
          connection.set("scm:git:git://github.com/ExoQuery/pprint-kotlin.git")
          developerConnection.set("scm:git:ssh://github.com:ExoQuery/pprint-kotlin.git")
        }
      }
    }
  }
}