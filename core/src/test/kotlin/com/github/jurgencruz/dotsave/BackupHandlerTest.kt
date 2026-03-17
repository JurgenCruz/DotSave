package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class BackupHandlerTest {
  @Test
  fun backupShouldFailIfNoDefaultAndNoProfileSelected() {
    val config = Config(listOf(Profile("program1", false, "root1", emptyList(), emptyList(), emptyList(), emptyList())))
    val result = BackupHandler.backup(config, "backup/dotsave.json", null, { _, _ -> }, { Result.success(Unit) }, { _, _ -> Result.success(Unit) }, { _ -> sequenceOf() })
    assertThat(result.exceptionOrNull()).hasMessage("No default profile in config file and no profile name specified")
  }
  @Test
  fun backupShouldFailIfNProfileSelectedDoesNotExist() {
    val config = Config(listOf(Profile("program1", false, "root1", emptyList(), emptyList(), emptyList(), emptyList())))
    val result = BackupHandler.backup(config, "backup/dotsave.json", "profile1", { _, _ -> }, { Result.success(Unit) }, { _, _ -> Result.success(Unit) }, { _ -> sequenceOf() })
    assertThat(result.exceptionOrNull()).hasMessage("No profile with name: profile1 exists")
  }
  @Test
  fun backupShouldCopyFilesToCorrectDestination() {
    val copyList = mutableListOf<Pair<Path, Path>>()
    val config = Config(listOf(Profile("program1", false, "root1", emptyList(), emptyList(), listOf("file1", "folder2"), emptyList()), Profile("program2", false, "root2", emptyList(), emptyList(), listOf("file3", "folder4"), emptyList())))
    val result = BackupHandler.backup(config, "backup/dotsave.json", "program1", { _, _ -> }, { Result.success(Unit) }, { p1, p2 -> Result.success(copyList.add(p1 to p2)).map { } }, { _ -> sequenceOf() })
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(copyList).hasSize(2)
    assertThat(copyList).zipSatisfy(listOf("root1/file1" to "backup/program1/file1", "root1/folder2" to "backup/program1/folder2")) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }
  @Test
  fun backupShouldFailIfCannotCreateDestinationDir() {
    val config = Config(listOf(Profile("test1", false, "root1", emptyList(), emptyList(), listOf("file1", "folder1"), emptyList())))
    val result = BackupHandler.backup(config, "backup/dotsave.json", "test1", { _, _ -> }, { Result.failure(IllegalAccessException("test")) }, { _, _ -> Result.success(Unit) }, { _ -> sequenceOf() })
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }
  @Test
  fun backupShouldFailIfCannotCopyAFile() {
    val config = Config(listOf(Profile("program1", false, "root1", emptyList(), emptyList(), listOf("file1", "folder2"), emptyList()), Profile("program2", false, "root2", emptyList(), emptyList(), listOf("file3", "folder4"), emptyList())))
    val result = BackupHandler.backup(config, "backup/dotsave.json", "program1", { _, _ -> }, { Result.success(Unit) }, { _, _ -> Result.failure(IllegalAccessException("test")) }, { _ -> sequenceOf() })
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }
  @Test
  fun backupShouldAggregateErrorsAndContinue() {
    val config = Config(listOf(Profile("program1", false, "root1", emptyList(), emptyList(), listOf("file1", "folder2", "file3", "folder4"), emptyList())))
    var i = 0
    val result = BackupHandler.backup(config, "backup/dotsave.json", "program1", { _, _ -> }, { Result.success(Unit) }, { _, _ -> Result.failure(IllegalAccessException("test${++i}")) }, { _ -> sequenceOf() })
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("test1")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("test2", "test3", "test4")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }
  @Test
  fun backupShouldRunIncludedProfiles() {
    val copyList = mutableListOf<Pair<Path, Path>>()
    val config = Config(listOf(Profile("program1", true, "root1", listOf("include1"), emptyList(), listOf("file1", "folder2"), listOf("missing1")), Profile("include1", false, "root2", listOf("include2"), emptyList(), listOf("file3", "folder4"), listOf("missing1")), Profile("include2", false, "root3", emptyList(), emptyList(), listOf("file5", "folder6"), listOf("missing1"))))
    val result = BackupHandler.backup(config, "backup/dotsave.json", null, { _, _ -> }, { Result.success(Unit) }, { p1, p2 -> Result.success(copyList.add(p1 to p2)).map { } }, { a -> sequenceOf(a.resolve("missing1"), a.resolve("missing2")) })
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow()).containsExactly("root1/missing2", "root2/missing2", "root3/missing2")
    assertThat(copyList).hasSize(6)
    assertThat(copyList).zipSatisfy(listOf("root3/file5" to "backup/include2/file5", "root3/folder6" to "backup/include2/folder6", "root2/file3" to "backup/include1/file3", "root2/folder4" to "backup/include1/folder4", "root1/file1" to "backup/program1/file1", "root1/folder2" to "backup/program1/folder2")) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }
  @Test
  fun backupShouldMergeInheritedProfiles() {
    val copyList = mutableListOf<Pair<Path, Path>>()
    val config = Config(listOf(Profile("program1", true, "root1", emptyList(), listOf("inherit1"), listOf("file1", "folder2"), listOf("missing1", "missing2")), Profile("inherit1", false, "root1/sub2", listOf("include1"), emptyList(), listOf("file3", "folder4"), emptyList()), Profile("include1", false, "root3", emptyList(), emptyList(), listOf("file5", "folder6"), emptyList())))
    val result = BackupHandler.backup(config, "backup/dotsave.json", null, { _, _ -> }, { Result.success(Unit) }, { p1, p2 -> Result.success(copyList.add(p1 to p2)).map { } }, { a -> sequenceOf(a.resolve("missing1"), a.resolve("missing2")) })
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow()).containsExactly("root3/missing1", "root3/missing2")
    assertThat(copyList).hasSize(6)
    assertThat(copyList).zipSatisfy(listOf("root3/file5" to "backup/include1/file5", "root3/folder6" to "backup/include1/folder6", "root1/file1" to "backup/program1/file1", "root1/folder2" to "backup/program1/folder2", "root1/sub2/file3" to "backup/program1/sub2/file3", "root1/sub2/folder4" to "backup/program1/sub2/folder4")) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }
}
