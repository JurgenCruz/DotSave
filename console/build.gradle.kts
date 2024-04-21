plugins {
  application
  idea
  jacoco
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt)
}
val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
  repositories(RepositoryHandler::mavenCentral)
  kapt(libs.dagger.compiler)
  implementation(project(":core"))
  implementation(project(":filesystemdataaccess"))
  implementation(libs.dagger)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit)
  testImplementation(libs.bundles.test)
  testRuntimeOnly(libs.junit.engine)
  mockitoAgent(libs.mockito) { isTransitive = false }
}

application {
  mainClass.set("com.github.jurgencruz.dotsave.Application")
}
val runningDir = project.file("./build/run/")

tasks {
  withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
  named<JavaExec>("run") {
    workingDir = runningDir
    dependsOn("createRunDir")
  }
  test {
    useJUnitPlatform()
    testLogging {
      events("PASSED", "FAILED", "SKIPPED")
    }
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
  }
  register("createRunDir") {
    doLast {
      runningDir.mkdirs()
    }
    dependsOn("compileJava")
  }
  jacocoTestReport {
    dependsOn(test)
  }
  check {
    dependsOn(jacocoTestReport)
  }
  register("fatJar", Jar::class) {
    manifest {
      attributes("Main-Class" to "com.github.jurgencruz.dotsave.Application")
    }
    archiveBaseName.set("dotsave")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
      configurations.runtimeClasspath.get().map {
        if (it.isDirectory()) it else zipTree(it)
      }
    })
    with(jar.get() as CopySpec)
  }
  register("fatJarScripts", CreateStartScripts::class) {
    applicationName = "dotsave"
    classpath = named("fatJar").get().outputs.files
    mainClass = "com.github.jurgencruz.dotsave.Application"
    outputDir = file("build/fatJarScripts")
    executableDir = ""
  }
}

distributions {
  create("fatJar") {
    distributionBaseName = "dotsave"
    contents {
      from(tasks.named("fatJar")) {
        into("lib")
      }
      from(tasks.named("fatJarScripts"))
      from("icon.svg")
    }
  }
}
