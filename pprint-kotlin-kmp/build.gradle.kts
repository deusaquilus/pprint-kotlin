import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization") version "1.9.22"

  //`maven-publish`
  // id("io.github.gradle-nexus.publish-plugin") version "1.1.0" // needs to be root
  signing
}

kotlin {
  jvm {
    jvmToolchain(11)
  }

  linuxX64()
  macosX64()
  mingwX64()

  js {
    browser()
    nodejs()
  }

  sourceSets {
    commonMain {
      dependencies {
        api(project(":pprint-kotlin-core"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
      }
    }

    commonTest {
      dependencies {
        // Used to ad-hoc some examples but not needed.
        //api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    jvmMain {
      dependencies {
        api(kotlin("reflect"))
      }
    }

    jvmTest {
      dependencies {
        implementation("io.kotest:kotest-runner-junit5:5.8.0")
      }
    }
  }
}


tasks.named<Test>("jvmTest") {
  useJUnitPlatform()
}

tasks.withType<AbstractTestTask>().configureEach {
  testLogging {
    showStandardStreams = true
    showExceptions = true
    exceptionFormat = TestExceptionFormat.FULL
    events(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
  }
}

