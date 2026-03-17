package com.github.jurgencruz.dotsave.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProfileTest {
  @Test
  fun validateShouldReturnErrorIfNameIsEmpty() {
    val profile = Profile("", "", emptyList(), emptyList())
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Profile name cannot be blank. If using Env Vars, make sure they have valid values.")
  }
  @Test
  fun validateShouldReturnErrorIfRootIsEmpty() {
    val profile = Profile("name", "", emptyList(), emptyList())
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Root cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }
  @Test
  fun validateShouldReturnErrorIfIncludeAndIncludeProfilesAndInheritProfilesAreEmpty() {
    val profile = Profile("name", "root", emptyList(), emptyList())
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Either include, includeProfiles or inheritProfiles must include at least 1 item. Profile: name.")
  }
  @Test
  fun validateShouldReturnErrorIfIncludeItemIsEmpty() {
    val profile = Profile("name", "root", listOf(""), emptyList())
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Include items cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }
  @Test
  fun validateShouldReturnErrorIfIncludeProfilesItemIsEmpty() {
    val profile = Profile("name", "root", emptyList(), emptyList(), listOf(""))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("IncludeProfile items cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }
  @Test
  fun validateShouldReturnErrorIfInheritProfilesItemIsEmpty() {
    val profile = Profile("name", "root", emptyList(), emptyList(), inheritProfiles = listOf(""))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("InheritProfile items cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }
  @Test
  fun validateShouldReturnErrorIfIgnoreItemIsEmpty() {
    val profile = Profile("name", "root", listOf("a"), listOf(""))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Ignore items cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }
  @Test
  fun validateShouldReturnNoErrorIfValid() {
    val profile = Profile("name", "root", listOf("a"), emptyList())
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).isNull()
  }
}