package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.logging.Logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
class RestoreHandlerTest {
  private lateinit var mTarget: RestoreHandler

  @Mock
  private lateinit var mFileSystem: FileSystem

  @Mock
  private lateinit var mLogger: Logger

  @BeforeEach
  fun setup() {
    mTarget = RestoreHandler(mFileSystem, mLogger)
  }

  @Test
  fun restoreShouldCopyFilesToCorrectDestination() {
    whenever(mFileSystem.copy(any(), any())).thenReturn(Result.success(Unit))
    val config = Config(listOf(Profile("program1", "root1", listOf("file1", "folder2")), Profile("program2", "root2", listOf("file3", "folder4"))))
    val result = mTarget.restore(config, "backup/dotsave.json")
    assertThat(result.isSuccess).isTrue()
    val srcCaptor = argumentCaptor<Path>()
    val destCaptor = argumentCaptor<Path>()
    verify(mFileSystem, times(4)).copy(srcCaptor.capture(), destCaptor.capture())
    assertThat(srcCaptor.allValues).zipSatisfy(listOf("backup/program1/file1", "backup/program1/folder2", "backup/program2/file3", "backup/program2/folder4")) { a, b ->
      assertThat(a).hasToString(b)
    }
    assertThat(destCaptor.allValues).zipSatisfy(listOf("root1/file1", "root1/folder2", "root2/file3", "root2/folder4")) { a, b ->
      assertThat(a).hasToString(b)
    }
  }

  @Test
  fun restoreShouldFailIfCannotCopyAFile() {
    whenever(mFileSystem.copy(any(), any())).thenReturn(Result.failure(IllegalAccessException("test")))
    val config = Config(listOf(Profile("program1", "root1", listOf("file1", "folder2")), Profile("program2", "root2", listOf("file3", "folder4"))))
    val result = mTarget.restore(config, "backup/dotsave.json")
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }

  @Test
  fun backupShouldAggregateErrorsAndContinue() {
    whenever(mFileSystem.copy(any(), any())).thenReturn(Result.failure(IllegalAccessException("test1")), Result.failure(IllegalAccessException("test2")), Result.failure(IllegalAccessException("test3")), Result.failure(IllegalAccessException("test4")))
    val config = Config(listOf(Profile("program1", "root1", listOf("file1", "folder2")), Profile("program2", "root2", listOf("file3", "folder4"))))
    val result = mTarget.restore(config, "backup/dotsave.json")
    assertThat(result.isFailure).isTrue()
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("test1")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("test2", "test3", "test4")) { a, b ->
      assertThat(a).hasMessage(b)
    }
    val srcCaptor = argumentCaptor<Path>()
    val destCaptor = argumentCaptor<Path>()
    verify(mFileSystem, times(4)).copy(srcCaptor.capture(), destCaptor.capture())
    assertThat(srcCaptor.allValues).zipSatisfy(listOf("backup/program1/file1", "backup/program1/folder2", "backup/program2/file3", "backup/program2/folder4")) { a, b ->
      assertThat(a).hasToString(b)
    }
    assertThat(destCaptor.allValues).zipSatisfy(listOf("root1/file1", "root1/folder2", "root2/file3", "root2/folder4")) { a, b ->
      assertThat(a).hasToString(b)
    }
  }
}
