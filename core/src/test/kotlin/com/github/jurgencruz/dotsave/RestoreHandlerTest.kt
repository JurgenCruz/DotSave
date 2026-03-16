package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class RestoreHandlerTest {
  @Test
  fun restoreShouldCopyFilesToCorrectDestination() {
    val copyList = mutableListOf<Pair<Path, Path>>()
    val config = Config(listOf(Profile("program1", false, "root1", emptyList(), emptyList(), listOf("file1", "folder2"), emptyList()), Profile("program2", false, "root2", emptyList(), emptyList(), listOf("file3", "folder4"), emptyList())))
    val result = RestoreHandler.restore(config, "backup/dotsave.json", null, { _, _ -> }, { p1, p2 -> Result.success(copyList.add(p1 to p2)).map { } })
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(copyList).hasSize(4)
    assertThat(copyList).zipSatisfy(listOf("root1/file1" to "backup/program1/file1", "root1/folder2" to "backup/program1/folder2", "root2/file3" to "backup/program2/file3", "root2/folder4" to "backup/program2/folder4")) { (srcPath, destPath), (expectedDest, expectedSrc) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(destPath).hasToString(expectedDest)
    }
  }
  @Test
  fun restoreShouldFailIfCannotCopyAFile() {
    val config = Config(listOf(Profile("program1", false, "root1", emptyList(), emptyList(), listOf("file1", "folder2"), emptyList()), Profile("program2", false, "root2", emptyList(), emptyList(), listOf("file3", "folder4"), emptyList())))
    val result = RestoreHandler.restore(config, "backup/dotsave.json", null, { _, _ -> }, { _, _ -> Result.failure(IllegalAccessException("test")) })
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }
  @Test
  fun backupShouldAggregateErrorsAndContinue() {
    val config = Config(listOf(Profile("program1", false, "root1", emptyList(), emptyList(), listOf("file1", "folder2"), emptyList()), Profile("program2", false, "root2", emptyList(), emptyList(), listOf("file3", "folder4"), emptyList())))
    var i = 0
    val result = RestoreHandler.restore(config, "backup/dotsave.json", null, { _, _ -> }, { _, _ -> Result.failure(IllegalAccessException("test${++i}")) })
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("test1")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("test2", "test3", "test4")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }
}
