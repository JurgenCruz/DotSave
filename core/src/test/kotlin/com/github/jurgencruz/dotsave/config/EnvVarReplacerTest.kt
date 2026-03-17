package com.github.jurgencruz.dotsave.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class EnvVarReplacerTest {
  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      EnvVarReplacer.getEnv = ::mockGetEnv
    }

    fun mockGetEnv(varName: String): String {
      return if (varName == "exception") {
        throw SecurityException("fail")
      } else {
        "(replaced-$varName)"
      }
    }
  }
  @Test
  fun replaceEnvVarsShouldReplaceEnvVarInName() {
    val config = Config(listOf(Profile($$"${name1}s", "root", emptyList(), emptyList())))
    val result = EnvVarReplacer.replaceEnvVars(config)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().profiles[0].name).isEqualTo("(replaced-name1)s")
  }
  @Test
  fun replaceEnvVarsShouldReturnErrorIfInvalidName() {
    val config = Config(listOf(Profile($$"${exception}s", "root", emptyList(), emptyList())))
    val result = EnvVarReplacer.replaceEnvVars(config)
    assertThat(result.exceptionOrNull()).hasMessage("fail")
  }
  @Test
  fun replaceEnvVarsShouldReplaceEnvVarInRoot() {
    val config = Config(listOf(Profile("name", $$"${root1}s", emptyList(), emptyList())))
    val result = EnvVarReplacer.replaceEnvVars(config)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().profiles[0].root).isEqualTo("(replaced-root1)s")
  }
  @Test
  fun replaceEnvVarsShouldReturnErrorIfInvalidRoot() {
    val config = Config(listOf(Profile("name", $$"${exception}s", emptyList(), emptyList())))
    val result = EnvVarReplacer.replaceEnvVars(config)
    assertThat(result.exceptionOrNull()).hasMessage("fail")
  }
  @Test
  fun replaceEnvVarsShouldReplaceEnvVarInIncludeProfiles() {
    val config = Config(listOf(Profile("name", "root", emptyList(), emptyList(), listOf($$"${include1}s", $$"${include2}s"))))
    val result = EnvVarReplacer.replaceEnvVars(config)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().profiles[0].includeProfiles).containsExactly("(replaced-include1)s", "(replaced-include2)s")
  }
  @Test
  fun replaceEnvVarsShouldReturnErrorIfInvalidIncludeProfiles() {
    val config = Config(listOf(Profile("name", "root", emptyList(), emptyList(), listOf($$"${exception}s", $$"${exception}s", $$"${exception}s"))))
    val result = EnvVarReplacer.replaceEnvVars(config)
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("fail")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("fail", "fail")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }
  @Test
  fun replaceEnvVarsShouldReplaceEnvVarInInheritProfiles() {
    val config = Config(listOf(Profile("name", "root", emptyList(), emptyList(), inheritProfiles = listOf($$"${inherit1}s", $$"${inherit2}s"))))
    val result = EnvVarReplacer.replaceEnvVars(config)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().profiles[0].inheritProfiles).containsExactly("(replaced-inherit1)s", "(replaced-inherit2)s")
  }
  @Test
  fun replaceEnvVarsShouldReturnErrorIfInvalidInheritProfiles() {
    val config = Config(listOf(Profile("name", "root", emptyList(), emptyList(), inheritProfiles = listOf($$"${exception}s", $$"${exception}s", $$"${exception}s"))))
    val result = EnvVarReplacer.replaceEnvVars(config)
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("fail")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("fail", "fail")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }
  @Test
  fun replaceEnvVarsShouldReplaceEnvVarInInclude() {
    val config = Config(listOf(Profile("name", "root", listOf($$"${include1}s", $$"${include2}s"), emptyList())))
    val result = EnvVarReplacer.replaceEnvVars(config)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().profiles[0].include).containsExactly("(replaced-include1)s", "(replaced-include2)s")
  }
  @Test
  fun replaceEnvVarsShouldReturnErrorIfInvalidInclude() {
    val config = Config(listOf(Profile("name", "root", listOf($$"${exception}s", $$"${exception}s", $$"${exception}s"), emptyList())))
    val result = EnvVarReplacer.replaceEnvVars(config)
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("fail")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("fail", "fail")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }
  @Test
  fun replaceEnvVarsShouldReplaceEnvVarInIgnore() {
    val config = Config(listOf(Profile("name", "root", emptyList(), listOf($$"${ignore1}s", $$"${ignore2}s"))))
    val result = EnvVarReplacer.replaceEnvVars(config)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().profiles[0].ignore).containsExactly("(replaced-ignore1)s", "(replaced-ignore2)s")
  }
  @Test
  fun replaceEnvVarsShouldReturnErrorIfInvalidIgnore() {
    val config = Config(listOf(Profile("name", "root", emptyList(), listOf($$"${exception}s", $$"${exception}s", $$"${exception}s"))))
    val result = EnvVarReplacer.replaceEnvVars(config)
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("fail")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("fail", "fail")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }
}
