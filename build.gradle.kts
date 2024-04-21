plugins {
  idea
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
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
