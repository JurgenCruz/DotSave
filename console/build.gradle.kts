plugins {
  application
  idea
  jacoco
  kotlin("jvm")
  kotlin("kapt")
}

dependencies {
  repositories(RepositoryHandler::mavenCentral)
  kapt(rootProject.ext["dagger-compiler"].toString())
  implementation(project(":core"))
  implementation(project(":filesystemdataaccess"))
  implementation(rootProject.ext["dagger"].toString())
  testImplementation(rootProject.ext["mockito"].toString())
  testImplementation(rootProject.ext["mockito-extension"].toString())
  testImplementation(rootProject.ext["assertj"].toString())
  testImplementation(rootProject.ext["junit-api"].toString())
  testRuntimeOnly(rootProject.ext["junit-engine"].toString())
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
  create("createRunDir") {
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
  create("fatJar", Jar::class) {
    manifest {
      attributes("Main-Class" to "com.github.jurgencruz.dotsave.Application")
    }
    archiveBaseName.set("dotsave")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory()) it else zipTree(it) })
    with(jar.get() as CopySpec)
  }
  create("fatJarScripts", CreateStartScripts::class) {
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
    }
  }
}
