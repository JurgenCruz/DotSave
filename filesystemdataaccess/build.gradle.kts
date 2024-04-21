plugins {
  `java-library`
  idea
  jacoco
  kotlin("jvm")
}

dependencies {
  repositories(RepositoryHandler::mavenCentral)
  implementation(project(":core"))
  testImplementation(rootProject.ext["mockito"].toString())
  testImplementation(rootProject.ext["mockito-extension"].toString())
  testImplementation(rootProject.ext["assertj"].toString())
  testImplementation(rootProject.ext["junit-api"].toString())
  testRuntimeOnly(rootProject.ext["junit-engine"].toString())
}

tasks {
  test {
    useJUnitPlatform()
    testLogging {
      events("PASSED", "FAILED", "SKIPPED")
    }
  }
  compileJava {
    targetCompatibility = "11"
    sourceCompatibility = "11"
  }
  compileTestJava {
    targetCompatibility = "11"
  }
  compileKotlin {
    kotlinOptions {
      jvmTarget = "11"
    }
  }
  compileTestKotlin {
    kotlinOptions {
      jvmTarget = "11"
    }
  }
  jacocoTestReport {
    dependsOn(test)
  }
  check {
    dependsOn(jacocoTestReport)
  }
}
