package com.github.jurgencruz.dotsave.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class EnvVarReplacerTest {
  private lateinit var mTarget: MockEnvVarReplacer

  @BeforeEach
  fun setup() {
    mTarget = MockEnvVarReplacer()
  }

  @Test
  fun replaceShouldReplaceEnvVar() {
    var result = mTarget.replace("/\$HOME/")
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("/(replaced-HOME)/")
    result = mTarget.replace("/\${NAME_WITH_1_NUMBER}asdf/")
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("/(replaced-NAME_WITH_1_NUMBER)asdf/")
  }

  @Test
  fun replaceShouldReturnErrorIfSystemThrowsException() {
    val result = mTarget.replace("/\$exception/")
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("fail")
  }

  class MockEnvVarReplacer : EnvVarReplacer() {
    override fun getEnv(varName: String): String {
      return if (varName == "exception") {
        throw SecurityException("fail")
      } else {
        "(replaced-$varName)"
      }
    }
  }
}
