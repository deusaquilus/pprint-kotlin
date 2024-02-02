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

  publications {
    create<MavenPublication>("mavenJava") {
      from(components["kotlin"])
      artifactId = varintName

      artifact(tasks["javadocJar"])
      artifact(tasks["sourcesJar"])

      pom {
        name.set("decomat")
        description.set("DecoMat - Deconstructive Pattern Matching for Kotlin")
        url.set("https://github.com/exoquery/decomat")

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