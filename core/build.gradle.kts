plugins {
  `java-library`
  idea
  jacoco
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
}
val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
  repositories(RepositoryHandler::mavenCentral)
  implementation(libs.serialization.json)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit)
  testImplementation(libs.bundles.test)
  testRuntimeOnly(libs.junit.engine)
  mockitoAgent(libs.mockito) { isTransitive = false }
}

tasks {
  test {
    useJUnitPlatform()
    testLogging {
      events("PASSED", "FAILED", "SKIPPED")
    }
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
  }
  jacocoTestReport {
    dependsOn(test)
  }
  check {
    dependsOn(jacocoTestReport)
  }
}
