package com.github.jurgencruz.dotsave.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfigValidatorTest {
  @Test
  fun validateShouldReturnErrorIfProfilesContainDuplicateNames() {
    val config = Config(listOf(Profile("test", false, "root", emptyList(), emptyList(), emptyList(), emptyList()), Profile("test", false, "root2", emptyList(), emptyList(), emptyList(), emptyList())))
    val result = ConfigValidator.validate(config)
    assertThat(result.exceptionOrNull()).hasMessage("Config cannot contain two profiles with the same name")
  }
  @Test
  fun validateShouldReturnErrorIfProfilesContainMoreThanOneDefaultProfile() {
    val config = Config(listOf(Profile("test1", true, "root", emptyList(), emptyList(), emptyList(), emptyList()), Profile("test2", true, "root2", emptyList(), emptyList(), emptyList(), emptyList())))
    val result = ConfigValidator.validate(config)
    assertThat(result.exceptionOrNull()).hasMessage("Config cannot contain more than one default profile")
  }
  @Test
  fun validateShouldReturnErrorIfProfilesNameIsBlank() {
    val config = Config(listOf(Profile("", false, "root", emptyList(), emptyList(), emptyList(), emptyList())))
    val result = ConfigValidator.validate(config)
    assertThat(result.exceptionOrNull()).hasMessage("Profile names cannot be blank. If using Env Vars, make sure they have valid values")
  }
  @Test
  fun validateShouldReturnErrorIfProfilesIncludeMissingProfiles() {
    val config = Config(listOf(Profile("test", false, "root", listOf("child"), emptyList(), emptyList(), emptyList()), Profile("test2", false, "root2", emptyList(), emptyList(), emptyList(), emptyList())))
    val result = ConfigValidator.validate(config)
    assertThat(result.exceptionOrNull()).hasMessage("Profile references must exist")
  }
  @Test
  fun validateShouldReturnErrorIfProfilesInheritMissingProfiles() {
    val config = Config(listOf(Profile("test", false, "root", emptyList(), listOf("child"), emptyList(), emptyList()), Profile("test2", false, "root2", emptyList(), emptyList(), emptyList(), emptyList())))
    val result = ConfigValidator.validate(config)
    assertThat(result.exceptionOrNull()).hasMessage("Profile references must exist")
  }
  @Test
  fun validateShouldReturnConfigIfValid() {
    val config = Config(listOf(Profile("test", false, "root", listOf("include"), listOf("inherit"), emptyList(), emptyList()), Profile("include", false, "root2", emptyList(), emptyList(), emptyList(), emptyList()), Profile("inherit", false, "root3", emptyList(), emptyList(), emptyList(), emptyList())))
    val result = ConfigValidator.validate(config)
    assertThat(result.exceptionOrNull()).isNull()
  }
}
