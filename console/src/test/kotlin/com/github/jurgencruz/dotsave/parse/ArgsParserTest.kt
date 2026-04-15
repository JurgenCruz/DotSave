package com.github.jurgencruz.dotsave.parse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArgsParserTest {
  @Test
  fun parseResultActionShouldBeErrorIfInvalidOption() {
    val result = ArgsParser.parse(arrayOf("-x"))
    assertThat(result.exceptionOrNull()).hasMessage("Unrecognized argument: -x")
  }

  @Test
  fun parseResultActionShouldBeShowUsageIfNoOptions() {
    val result = ArgsParser.parse(emptyArray())
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
    assertThat(result.getOrThrow().path).isEqualTo("")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
  }

  @Test
  fun parseResultActionShouldBeShowUsageIfNoActions() {
    val result = ArgsParser.parse(arrayOf("-v", "-p", "profile"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
    assertThat(result.getOrThrow().path).isEqualTo("")
    assertThat(result.getOrThrow().profile).isEqualTo("profile")
    assertThat(result.getOrThrow().verbose).isTrue()
  }

  @Test
  fun parseResultActionShouldBeShowUsageIfSpecifiedFirst() {
    var result = ArgsParser.parse(arrayOf("-h"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
    assertThat(result.getOrThrow().path).isEqualTo("")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
    result = ArgsParser.parse(arrayOf("--help"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
    assertThat(result.getOrThrow().path).isEqualTo("")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
    result = ArgsParser.parse(arrayOf("-h", "-V", "-b"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.USAGE)
    assertThat(result.getOrThrow().path).isEqualTo("")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
  }

  @Test
  fun parseResultActionShouldBeShowVersionIfSpecifiedFirst() {
    var result = ArgsParser.parse(arrayOf("-V"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.VERSION)
    assertThat(result.getOrThrow().path).isEqualTo("")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
    result = ArgsParser.parse(arrayOf("--version"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.VERSION)
    assertThat(result.getOrThrow().path).isEqualTo("")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
    result = ArgsParser.parse(arrayOf("-V", "-h", "-b"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.VERSION)
    assertThat(result.getOrThrow().path).isEqualTo("")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
  }

  @Test
  fun parseResultActionShouldBeErrorIfNoSavePathSpecified() {
    var result = ArgsParser.parse(arrayOf("-b"))
    assertThat(result.exceptionOrNull()).hasMessage("No path specified for saving")
    result = ArgsParser.parse(arrayOf("--back-up"))
    assertThat(result.exceptionOrNull()).hasMessage("No path specified for saving")
  }

  @Test
  fun parseResultActionShouldBeSaveIfSaveSpecifiedAndConfPathSpecified() {
    var result = ArgsParser.parse(arrayOf("-b", "path1"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.BACKUP)
    assertThat(result.getOrThrow().path).isEqualTo("path1")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
    result = ArgsParser.parse(arrayOf("--back-up", "path2"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.BACKUP)
    assertThat(result.getOrThrow().path).isEqualTo("path2")
    assertThat(result.getOrThrow().profile).isNull()
    assertThat(result.getOrThrow().verbose).isFalse()
  }

  @Test
  fun parseResultActionShouldBeErrorIfNoApplyPathSpecified() {
    var result = ArgsParser.parse(arrayOf("-r"))
    assertThat(result.exceptionOrNull()).hasMessage("No path specified for restoring")
    result = ArgsParser.parse(arrayOf("--restore"))
    assertThat(result.exceptionOrNull()).hasMessage("No path specified for restoring")
  }

  @Test
  fun parseResultActionShouldBeApplyIfApplySpecifiedAndConfPathSpecified() {
    var result = ArgsParser.parse(arrayOf("-r", "path1"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.RESTORE)
    assertThat(result.getOrThrow().path).isEqualTo("path1")
    result = ArgsParser.parse(arrayOf("--restore", "path2"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.RESTORE)
    assertThat(result.getOrThrow().path).isEqualTo("path2")
  }

  @Test
  fun parseResultActionShouldBeErrorIfTwoActionsSpecified() {
    var result = ArgsParser.parse(arrayOf("-r", "path", "-b", "path"))
    assertThat(result.exceptionOrNull()).hasMessage("You can only specify one action")
    result = ArgsParser.parse(arrayOf("--back-up", "path", "--restore", "path"))
    assertThat(result.exceptionOrNull()).hasMessage("You can only specify one action")
  }

  @Test
  fun parseResultActionShouldBeErrorIfNoProfileSpecifiedAfterFlag() {
    var result = ArgsParser.parse(arrayOf("-b", "path1", "-p"))
    assertThat(result.exceptionOrNull()).hasMessage("No profile name specified")
    result = ArgsParser.parse(arrayOf("--back-up", "path2", "--profile"))
    assertThat(result.exceptionOrNull()).hasMessage("No profile name specified")
  }

  @Test
  fun parseResultActionShouldSetProfileIfSpecified() {
    var result = ArgsParser.parse(arrayOf("-b", "path1", "-p", "profile1"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.BACKUP)
    assertThat(result.getOrThrow().path).isEqualTo("path1")
    assertThat(result.getOrThrow().verbose).isFalse()
    assertThat(result.getOrThrow().profile).isEqualTo("profile1")
    result = ArgsParser.parse(arrayOf("-r", "path2", "--profile", "profile2", "-v"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.RESTORE)
    assertThat(result.getOrThrow().path).isEqualTo("path2")
    assertThat(result.getOrThrow().verbose).isTrue()
    assertThat(result.getOrThrow().profile).isEqualTo("profile2")
  }

  @Test
  fun parseResultActionShouldSetVerboseIfBackupOrRestore() {
    var result = ArgsParser.parse(arrayOf("-b", "path1", "-v"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.BACKUP)
    assertThat(result.getOrThrow().path).isEqualTo("path1")
    assertThat(result.getOrThrow().verbose).isTrue()
    assertThat(result.getOrThrow().profile).isNull()
    result = ArgsParser.parse(arrayOf("-r", "path2", "-v"))
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().action).isEqualTo(Action.RESTORE)
    assertThat(result.getOrThrow().path).isEqualTo("path2")
    assertThat(result.getOrThrow().verbose).isTrue()
    assertThat(result.getOrThrow().profile).isNull()
  }
}
