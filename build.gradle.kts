plugins {
  idea
  kotlin("jvm") version "1.9.23" apply false
  kotlin("kapt") version "1.9.23" apply false
  kotlin("plugin.serialization") version "1.9.23" apply false
}

allprojects {
  group = "com.github.jurgencruz.dotsave"
  version = "1.0.0"
  apply {
    plugin("idea")
  }
  idea {
    module {
      isDownloadJavadoc = true
      isDownloadSources = true
    }
  }
}
val junitg = "org.junit.jupiter"
val junitv = "5.10.2"
val mockitog = "org.mockito"
val mockitov = "5.11.0"
val daggerg = "com.google.dagger"
val daggerv = "2.51.1"

ext {
  this["kotlin-serialization"] = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3"
  this["junit-api"] = "$junitg:junit-jupiter-api:$junitv"
  this["junit-engine"] = "$junitg:junit-jupiter-engine:$junitv"
  this["mockito"] = "$mockitog:mockito-core:$mockitov"
  this["mockito-kotlin"] = "$mockitog.kotlin:mockito-kotlin:5.3.1"
  this["mockito-extension"] = "$mockitog:mockito-junit-jupiter:$mockitov"
  this["assertj"] = "org.assertj:assertj-core:3.25.3"
  this["dagger"] = "$daggerg:dagger:$daggerv"
  this["dagger-compiler"] = "$daggerg:dagger-compiler:$daggerv"
}
