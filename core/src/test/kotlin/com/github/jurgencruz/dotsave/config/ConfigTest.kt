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
    val config = Config(listOf(Profile("test", "root", listOf("a"), emptyList()), Profile("test", "root2", listOf("a"), emptyList())))
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Config cannot contain two profiles with the same name")
  }
  @Test
  fun validateShouldReturnErrorIfProfilesContainMoreThanOneDefaultProfile() {
    val config = Config(listOf(Profile("test1", "root", listOf("a"), emptyList(), default = true), Profile("test2", "root2", listOf("a"), emptyList(), default = true)))
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Config cannot contain more than one default profile")
  }
  @Test
  fun validateShouldReturnErrorIfProfilesIncludeMissingProfiles() {
    val config = Config(listOf(Profile("test", "root", emptyList(), emptyList(), listOf("child")), Profile("test2", "root2", listOf("a"), emptyList())))
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Profile references must exist")
  }
  @Test
  fun validateShouldReturnErrorIfProfilesInheritMissingProfiles() {
    val config = Config(listOf(Profile("test", "root", emptyList(), emptyList(), inheritProfiles = listOf("child")), Profile("test2", "root2", listOf("a"), emptyList())))
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Profile references must exist")
  }
  @Test
  fun validateShouldReturnConfigIfValid() {
    val config = Config(listOf(Profile("test", "root", listOf("a"), emptyList(), listOf("include"), listOf("inherit")), Profile("include", "root2", listOf("b"), emptyList()), Profile("inherit", "root3", listOf("c"), emptyList())))
    val result = runCatching { config.validate() }
    assertThat(result.exceptionOrNull()).isNull()
  }
}
