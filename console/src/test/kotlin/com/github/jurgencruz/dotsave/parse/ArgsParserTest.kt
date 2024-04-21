package com.github.jurgencruz.dotsave.parse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArgsParserTest {
  private lateinit var mTarget: ArgsParser

  @BeforeEach
  fun setup() {
    mTarget = ArgsParser()
  }

  @Test
  fun parseResultActionShouldBeErrorIfInvalidOption() {
    val result = mTarget.parse(arrayOf("-x"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("Unrecognized argument: -x")
  }

  @Test
  fun parseResultActionShouldBeShowUsageIfNoOptions() {
    val result = mTarget.parse(emptyArray())
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
  }

  @Test
  fun parseResultActionShouldBeShowUsageIfSpecifiedFirst() {
    var result = mTarget.parse(arrayOf("-h"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
    result = mTarget.parse(arrayOf("--help"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
    result = mTarget.parse(arrayOf("-h", "-V", "-b"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
  }

  @Test
  fun parseResultActionShouldBeShowVersionIfSpecifiedFirst() {
    var result = mTarget.parse(arrayOf("-V"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.VERSION)
    result = mTarget.parse(arrayOf("--version"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.VERSION)
    result = mTarget.parse(arrayOf("-V", "-h", "-b"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.VERSION)
  }

  @Test
  fun parseResultActionShouldBeErrorIfNoSavePathSpecified() {
    var result = mTarget.parse(arrayOf("-b"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("No path specified for saving")
    result = mTarget.parse(arrayOf("--back-up"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("No path specified for saving")
  }

  @Test
  fun parseResultActionShouldBeSaveIfSaveSpecifiedAndConfPathSpecified() {
    var result = mTarget.parse(arrayOf("-b", "path1"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.BACKUP)
    assertThat(result.getOrThrow().path).isEqualTo("path1")
    result = mTarget.parse(arrayOf("--back-up", "path2"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.BACKUP)
    assertThat(result.getOrThrow().path).isEqualTo("path2")
  }

  @Test
  fun parseResultActionShouldBeErrorIfNoApplyPathSpecified() {
    var result = mTarget.parse(arrayOf("-r"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("No path specified for applying")
    result = mTarget.parse(arrayOf("--restore"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("No path specified for applying")
  }

  @Test
  fun parseResultActionShouldBeApplyIfApplySpecifiedAndConfPathSpecified() {
    var result = mTarget.parse(arrayOf("-r", "path1"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.RESTORE)
    assertThat(result.getOrThrow().path).isEqualTo("path1")
    result = mTarget.parse(arrayOf("--restore", "path2"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.RESTORE)
    assertThat(result.getOrThrow().path).isEqualTo("path2")
  }

  @Test
  fun parseResultActionShouldBeErrorIfTwoActionsSpecified() {
    var result = mTarget.parse(arrayOf("-b", "path1", "-v"))
    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow().action).isEqualTo(Action.BACKUP)
    assertThat(result.getOrThrow().path).isEqualTo("path1")
    result = mTarget.parse(arrayOf("-r", "path", "-b", "path"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("You can only specify one action")
    result = mTarget.parse(arrayOf("--back-up", "path", "-restore", "path"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("You can only specify one action")
  }
}
