package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

class RestoreHandlerTest {
  @Test
  fun restoreShouldCopyFilesToCorrectDestination() {
    val copyList = mutableListOf<Pair<Path, Path>>()
    val config = Config(listOf(Profile("program1", "root1", listOf("file1", "folder2/")), Profile("program2", "root2", listOf("file3", "folder4/"))))
    val result = RestoreHandler.restore(config, config.profiles[0], Path("backup"), { _, _ -> }, { p1, p2 -> Result.success(copyList.add(p1 to p2)).map { } })
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(copyList).hasSize(2)
    assertThat(copyList).zipSatisfy(listOf("backup/program1/file1" to "root1/file1", "backup/program1/folder2" to "root1/folder2")) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }

  @Test
  fun restoreShouldFailIfCannotCopyAFile() {
    val config = Config(listOf(Profile("program1", "root1", listOf("file1", "folder2/")), Profile("program2", "root2", listOf("file3", "folder4/"))))
    val result = RestoreHandler.restore(config, config.profiles[0], Path("backup"), { _, _ -> }, { _, _ -> Result.failure(IllegalAccessException("test")) })
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }

  @Test
  fun restoreShouldAggregateErrorsAndContinue() {
    val config = Config(listOf(Profile("program1", "root1", listOf("file1", "folder2/", "file3", "folder4/"))))
    var i = 0
    val result = RestoreHandler.restore(config, config.profiles[0], Path("backup"), { _, _ -> }, { _, _ -> Result.failure(IllegalAccessException("test${++i}")) })
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("test1")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("test2", "test3", "test4")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }

  @Test
  fun restoreShouldRunIncludedProfiles() {
    val copyList = mutableListOf<Pair<Path, Path>>()
    val config = Config(listOf(Profile("program1", "root1", listOf("file1", "folder2/"), listOf("missing1"), listOf("include1"), default = true), Profile("include1", "root2", listOf("file3", "folder4/"), listOf("missing1"), listOf("include2")), Profile("include2", "root3", listOf("file5", "folder6/"), listOf("missing1"))))
    val result = RestoreHandler.restore(config, config.profiles[0], Path("backup"), { _, _ -> }, { p1, p2 -> Result.success(copyList.add(p1 to p2)).map { } })
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(copyList).hasSize(6)
    assertThat(copyList).zipSatisfy(listOf("backup/include2/file5" to "root3/file5", "backup/include2/folder6" to "root3/folder6", "backup/include1/file3" to "root2/file3", "backup/include1/folder4" to "root2/folder4", "backup/program1/file1" to "root1/file1", "backup/program1/folder2" to "root1/folder2")) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }

  @Test
  fun restoreShouldMergeInheritedProfiles() {
    val copyList = mutableListOf<Pair<Path, Path>>()
    val config = Config(listOf(Profile("program1", "root1", listOf("file1", "folder2/"), listOf("missing1", "missing2"), inheritProfiles = listOf("inherit1"), default = true), Profile("inherit1", "root1/sub2", listOf("file3", "folder4/"), includeProfiles = listOf("include1")), Profile("include1", "root3", listOf("file5", "folder6/"))))
    val result = RestoreHandler.restore(config, config.profiles[0], Path("backup"), { _, _ -> }, { p1, p2 -> Result.success(copyList.add(p1 to p2)).map { } })
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(copyList).hasSize(6)
    assertThat(copyList).zipSatisfy(listOf("backup/include1/file5" to "root3/file5", "backup/include1/folder6" to "root3/folder6", "backup/program1/file1" to "root1/file1", "backup/program1/folder2" to "root1/folder2", "backup/program1/sub2/file3" to "root1/sub2/file3", "backup/program1/sub2/folder4" to "root1/sub2/folder4")) { (srcPath, destPath), (expectedSrc, expectedDest) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }
}
