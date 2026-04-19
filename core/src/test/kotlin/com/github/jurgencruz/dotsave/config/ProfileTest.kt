package com.github.jurgencruz.dotsave.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProfileTest {
  @Test
  fun validateShouldReturnErrorIfNameIsEmpty() {
    val profile = Profile("", "")
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Profile name cannot be blank. If using Env Vars, make sure they have valid values.")
  }

  @Test
  fun validateShouldReturnErrorIfNameFormsAnAbsolutePath() {
    val profile = Profile("/", "")
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Profile name contains invalid characters as it will be used as path. Profile: /")
  }

  @Test
  fun validateShouldReturnErrorIfRootIsEmpty() {
    val profile = Profile("name", "")
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Root cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }

  @Test
  fun validateShouldReturnErrorIfRootIsNotAbsolute() {
    val profile = Profile("name", "root")
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Root must be an absolute path. Profile: name")
  }

  @Test
  fun validateShouldReturnErrorIfIncludeAndIgnoreAndIncludeProfilesAndInheritProfilesAreEmpty() {
    val profile = Profile("name", "/root")
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Either include, ignore, includeProfiles or inheritProfiles must include at least 1 item. Profile: name.")
  }

  @Test
  fun validateShouldReturnErrorIfIncludeItemIsEmpty() {
    val profile = Profile("name", "/root", listOf(""))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Include items cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }

  @Test
  fun validateShouldReturnErrorIfIncludeItemIsAbsolute() {
    val profile = Profile("name", "/root", listOf("/a"))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Include paths must be relative: \"/a\". Profile: name")
  }

  @Test
  fun validateShouldReturnErrorIfIncludeProfilesItemIsEmpty() {
    val profile = Profile("name", "/root", includeProfiles = listOf(""))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("IncludeProfile items cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }

  @Test
  fun validateShouldReturnErrorIfInheritProfilesItemIsEmpty() {
    val profile = Profile("name", "/root", inheritProfiles = listOf(""))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("InheritProfile items cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }

  @Test
  fun validateShouldReturnErrorIfIgnoreItemIsEmpty() {
    val profile = Profile("name", "/root", listOf("a"), listOf(""))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Ignore items cannot be blank. Profile: name. If using Env Vars, make sure they have valid values.")
  }
  @Test
  fun validateShouldReturnErrorIfIgnoreItemIsAbsolute() {
    val profile = Profile("name", "/root", listOf("a"), listOf("/b"))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).hasMessage("Ignore paths must be relative: \"/b\". Profile: name")
  }

  @Test
  fun validateShouldReturnNoErrorIfValid() {
    val profile = Profile("name", "/root", listOf("a"))
    val result = runCatching { profile.validate() }
    assertThat(result.exceptionOrNull()).isNull()
  }
}