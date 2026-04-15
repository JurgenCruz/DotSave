package com.github.jurgencruz.dotsave.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfigTest {
  @Test
  fun validateShouldReturnErrorIfProfilesIsEmpty() {
    val config = Config(emptyList())
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("No profiles found in configuration.")
  }

  @Test
  fun validateShouldReturnErrorIfProfilesContainDuplicateNames() {
    val config = Config(listOf(Profile("test", "root", listOf("a")), Profile("test", "root2", listOf("a"))))
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Config cannot contain two profiles with the same name")
  }

  @Test
  fun validateShouldReturnErrorIfProfilesContainMoreThanOneDefaultProfile() {
    val config = Config(
      listOf(
        Profile("test1", "root", listOf("a"), default = true),
        Profile("test2", "root2", listOf("a"), default = true)
      )
    )
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Config cannot contain more than one default profile")
  }

  @Test
  fun validateShouldReturnErrorIfProfilesIncludeMissingProfiles() {
    val config = Config(listOf(Profile("test", "root", includeProfiles = listOf("child")), Profile("test2", "root2", listOf("a"))))
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Profile references must exist")
  }

  @Test
  fun validateShouldReturnErrorIfProfilesInheritMissingProfiles() {
    val config = Config(listOf(Profile("test", "root", inheritProfiles = listOf("child")), Profile("test2", "root2", listOf("a"))))
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Profile references must exist")
  }

  @Test
  fun validateShouldReturnConfigIfValid() {
    val config = Config(
      listOf(
        Profile("test", "root", listOf("a"), includeProfiles = listOf("include"), inheritProfiles = listOf("inherit")),
        Profile("include", "root2", listOf("b")), Profile("inherit", "root3", listOf("c"))
      )
    )
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).isNull()
  }

  @Test
  fun selectProfileShouldFailIfNoDefaultProfileAndNoNameSpecified() {
    val config = Config(listOf(Profile("program1", "root1")))
    val result = config.selectProfile(null)
    assertThat(result.exceptionOrNull()).hasMessage("No default profile in config file and no profile name specified")
  }

  @Test
  fun selectProfileShouldFailIfProfileSelectedDoesNotExist() {
    val config = Config(listOf(Profile("program1", "root1")))
    val result = config.selectProfile("profile1")
    assertThat(result.exceptionOrNull()).hasMessage("No profile with name: profile1 exists")
  }

  @Test
  fun selectProfileShouldReturnDefaultProfile() {
    val config = Config(listOf(Profile("program1", "root1", default = true)))
    val result = config.selectProfile(null)
    assertThat(result.getOrNull()).isEqualTo(config.profiles[0])
  }

  @Test
  fun selectProfileShouldReturnSelectedProfileByName() {
    val config = Config(listOf(Profile("program1", "root1"), Profile("program2", "root2", listOf("file3", "folder4/"))))
    val result = config.selectProfile("program2")
    assertThat(result.getOrNull()).isEqualTo(config.profiles[1])
  }
}
