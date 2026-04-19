package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.logging.LogLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

class BackupHandlerTest {
  val backupPath = Path("backup")
  val logStub = { _: LogLevel, _: String -> }
  val recreateDirStub = { _: Path -> Result.success(Unit) }
  val walkStub = { _: Path -> sequenceOf<Path>() }
  val copyStub = { _: Path, _: Path -> Result.success(Unit) }

  @Test
  fun backupShouldCopyFilesToCorrectDestination() {
    val config = Config(listOf(getProfile1(), getProfile2()))
    val copyList = mutableListOf<Pair<Path, Path>>()
    val copy = { p1: Path, p2: Path -> Result.success(copyList.add(p1 to p2)).map { } }
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, logStub, recreateDirStub, copy, walkStub)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(copyList).hasSize(2)
    assertThat(copyList).zipSatisfy(
      listOf(
        "/root1/file1" to "backup/program1/file1",
        "/root1/folder2" to "backup/program1/folder2"
      )
    ) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }

  @Test
  fun backupShouldFailIfCannotCreateDestinationDir() {
    val config = Config(listOf(getProfile1()))
    val recreateDir = { _: Path -> Result.failure<Unit>(IllegalAccessException("test")) }
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, logStub, recreateDir, copyStub, walkStub)
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }

  @Test
  fun backupShouldFailIfCannotCopyAFile() {
    val config = Config(listOf(getProfile1(), getProfile2()))
    val copy = { _: Path, _: Path -> Result.failure<Unit>(IllegalAccessException("test")) }
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, logStub, recreateDirStub, copy, walkStub)
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }

  @Test
  fun backupShouldAggregateErrorsAndContinue() {
    val config = Config(listOf(Profile("program1", "/root1", listOf("file1", "folder2", "file3", "folder4"))))
    var i = 0
    val copy = { _: Path, _: Path -> Result.failure<Unit>(IllegalAccessException("test${++i}")) }
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, logStub, recreateDirStub, copy, walkStub)
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("test1")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("test2", "test3", "test4")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }

  @Test
  fun backupShouldReturnListOfFilesNotBackedUpOrIgnored() {
    val config = Config(listOf(getProfile1()))
    val walk = { a: Path ->
      sequenceOf(
        a.resolve("file1"),
        a.resolve("folder2/f1"),
        a.resolve("folder2/f2"),
        a.resolve("file3"),
        a.resolve("folder4/f1"),
        a.resolve("folder4/f2"),
        a.resolve("ignore1"),
        a.resolve("ignore2/f1"),
        a.resolve("ignore2/f2")
      )
    }
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, logStub, recreateDirStub, copyStub, walk)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().map(Path::toString)).containsExactly("/root1/file3", "/root1/folder4/f1", "/root1/folder4/f2")
  }

  @Test
  fun backupShouldRunIncludedProfiles() {
    val config = Config(
      listOf(
        Profile("program1", "/root1", listOf("file1", "folder2"), listOf("missing1"), listOf("include1")),
        Profile("include1", "/root2", listOf("file3", "folder4"), listOf("missing1"), listOf("include2")),
        Profile("include2", "/root3", listOf("file5", "folder6"), listOf("missing1"))
      )
    )
    val copyList = mutableListOf<Pair<Path, Path>>()
    val copy = { p1: Path, p2: Path -> Result.success(copyList.add(p1 to p2)).map { } }
    val walk = { a: Path -> sequenceOf(a.resolve("missing1"), a.resolve("missing2")) }
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, logStub, recreateDirStub, copy, walk)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().map(Path::toString)).containsExactly("/root1/missing2", "/root2/missing2", "/root3/missing2")
    assertThat(copyList).hasSize(6)
    assertThat(copyList).zipSatisfy(
      listOf(
        "/root3/file5" to "backup/include2/file5",
        "/root3/folder6" to "backup/include2/folder6",
        "/root2/file3" to "backup/include1/file3",
        "/root2/folder4" to "backup/include1/folder4",
        "/root1/file1" to "backup/program1/file1",
        "/root1/folder2" to "backup/program1/folder2"
      )
    ) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }

  @Test
  fun backupShouldMergeInheritedProfiles() {
    val config = Config(
      listOf(
        Profile("program1", "/root1", listOf("file1", "folder2"), listOf("missing1", "missing2"), inheritProfiles = listOf("inherit1")),
        Profile("inherit1", "/root1/sub2", listOf("file3", "folder4"), includeProfiles = listOf("include1")),
        Profile("include1", "/root3", listOf("file5", "folder6"))
      )
    )
    val copyList = mutableListOf<Pair<Path, Path>>()
    val copy = { p1: Path, p2: Path -> Result.success(copyList.add(p1 to p2)).map { } }
    val walk = { a: Path -> sequenceOf(a.resolve("missing1"), a.resolve("missing2")) }
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, logStub, recreateDirStub, copy, walk)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().map(Path::toString)).containsExactly("/root3/missing1", "/root3/missing2")
    assertThat(copyList).hasSize(6)
    assertThat(copyList).zipSatisfy(
      listOf(
        "/root3/file5" to "backup/include1/file5",
        "/root3/folder6" to "backup/include1/folder6",
        "/root1/file1" to "backup/program1/file1",
        "/root1/folder2" to "backup/program1/folder2",
        "/root1/sub2/file3" to "backup/program1/sub2/file3",
        "/root1/sub2/folder4" to "backup/program1/sub2/folder4"
      )
    ) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }

  private fun getProfile1() = Profile("program1", "/root1", listOf("file1", "folder2"), listOf("ignore1", "ignore2/"))
  private fun getProfile2() = Profile("program2", "/root2", listOf("file3", "folder4"))
}
