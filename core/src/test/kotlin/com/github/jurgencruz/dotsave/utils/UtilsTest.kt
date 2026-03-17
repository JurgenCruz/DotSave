package com.github.jurgencruz.dotsave.utils

import com.github.jurgencruz.dotsave.config.Config
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtilsTest {
  @Test
  fun deserializeShouldReturnErrorIfEmptyString() {
    val result: Result<Config> = deserialize("")
    assertThat(result.exceptionOrNull()).hasMessageContaining("Expected start of the object '{', but had 'EOF' instead")
  }
  @Test
  fun deserializeShouldReturnErrorIfEmptyObject() {
    val result: Result<Config> = deserialize("{}")
    assertThat(result.exceptionOrNull()).hasMessageContaining("Field 'profiles' is required")
  }
  @Test
  fun deserializeShouldReturnErrorIfEmptyProfile() {
    val result: Result<Config> = deserialize("""{"profiles":[{}]}""")
    assertThat(result.exceptionOrNull()).hasMessageContaining("Fields [name, root, include, ignore] are required")
  }
  @Test
  fun deserializeShouldReturnConfigIfProfileIncludeIsNotEmpty() {
    val result: Result<Config> = deserialize("""{"profiles":[{"name":"name","root":"root","include":["a"],"ignore":[]}]}""")
    assertThat(result.exceptionOrNull()).isNull()
  }
  @Test
  fun deserializeShouldReturnConfigIfProfileIncludeProfilesIsNotEmpty() {
    val result: Result<Config> = deserialize("""{"profiles":[{"name":"name","root":"root","include":[],"ignore":[],"includeProfiles":["a"]}]}""")
    assertThat(result.exceptionOrNull()).isNull()
  }
  @Test
  fun deserializeShouldReturnConfigIfProfileInheritProfilesIsNotEmpty() {
    val result: Result<Config> = deserialize("""{"profiles":[{"name":"name","root":"root","include":[],"ignore":[],"inheritProfiles":["a"]}]}""")
    assertThat(result.exceptionOrNull()).isNull()
  }
}