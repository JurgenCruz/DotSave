package com.github.jurgencruz.dotsave.config

import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
class ConfigParserTest {
  private lateinit var mTarget: ConfigParser

  @Mock
  private lateinit var mFileSystem: FileSystem

  @Mock
  private lateinit var mEnVarReplacer: EnvVarReplacer

  @BeforeEach
  fun setup() {
    mTarget = ConfigParser(mFileSystem, mEnVarReplacer)
  }

  @Test
  fun parseShouldDeserializeConfigFile() {
    whenever(mFileSystem.read(Path.of("path/dotsave.json"))).thenReturn(Result.success("""{"profiles": [{"name": "test", "default": false, "root": "root", "includeProfiles": [], "inheritProfiles": [], "include": ["file", "folder"], "exclude": []},{"name": "test2", "default": false, "root": "root2", "includeProfiles": [], "inheritProfiles": [], "include": ["file2", "folder2"], "exclude": []}]}"""))
    whenever(mEnVarReplacer.replace(any())).thenAnswer { it.getArgument<String>(0) }
    whenever(mEnVarReplacer.replaceMandatory(any())).thenAnswer { it.getArgument<String>(0) }
    val result = mTarget.parse("path/dotsave.json")
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow()).isEqualTo(Config(listOf(Profile("test", false, "root", emptyList(), emptyList(), listOf("file", "folder"), emptyList()), Profile("test2", false, "root2", emptyList(), emptyList(), listOf("file2", "folder2"), emptyList()))))
  }

  @Test
  fun parseShouldReturnErrorIfFileSystemThrowsException() {
    whenever(mFileSystem.read(Path.of("path/dotsave.json"))).thenReturn(Result.failure(IllegalStateException("error")))
    val result = mTarget.parse("path/dotsave.json")
    assertThat(result.exceptionOrNull()).hasMessage("error")
  }

  @Test
  fun parseShouldReturnErrorIfDeserializationThrowsException() {
    whenever(mFileSystem.read(Path.of("path/dotsave.json"))).thenReturn(Result.success("""{"test": {"name": "test"}}"""))
    val result = mTarget.parse("path/dotsave.json")
    assertThat(result.exceptionOrNull()).hasMessageContaining("Encountered an unknown key 'test' at offset 2 at path")
  }

  @Test
  fun parseShouldReturnErrorIfProfilesContainDuplicateNames() {
    whenever(mFileSystem.read(Path.of("path/dotsave.json"))).thenReturn(Result.success("""{"profiles": [{"name": "test", "default": false, "root": "root", "includeProfiles": [], "inheritProfiles": [], "include": ["file", "folder"], "exclude": []},{"name": "test", "default": false, "root": "root2", "includeProfiles": [], "inheritProfiles": [], "include": ["file2", "folder2"], "exclude": []}]}"""))
    whenever(mEnVarReplacer.replace(any())).thenAnswer { it.getArgument<String>(0) }
    whenever(mEnVarReplacer.replaceMandatory(any())).thenAnswer { it.getArgument<String>(0) }
    val result = mTarget.parse("path/dotsave.json")
    assertThat(result.exceptionOrNull()).hasMessage("Config cannot contain two profiles with the same name")
  }

  @Test
  fun parseShouldReturnErrorIfVarSubstitutionFails() {
    whenever(mFileSystem.read(Path.of("path/dotsave.json"))).thenReturn(Result.success("""{"profiles": [{"name": "test1", "default": false, "root": "root1", "includeProfiles": [], "inheritProfiles": [], "include": ["file1", "folder1"], "exclude": []},{"name": "test2", "default": false, "root": "root2", "includeProfiles": [], "inheritProfiles": [], "include": ["file2", "folder2"], "exclude": []},{"name": "test3", "default": false, "root": "root3", "includeProfiles": [], "inheritProfiles": [], "include": ["file3", "folder3"], "exclude": []},{"name": "test4", "default": false, "root": "root4", "includeProfiles": [], "inheritProfiles": [], "include": ["file4", "folder4"], "exclude": []}]}"""))
    whenever(mEnVarReplacer.replace(anyString())).thenReturn(Result.failure(IllegalStateException("test1")), Result.success("test2"), Result.failure(IllegalStateException("root2")), Result.success("test3"), Result.success("root3"), Result.failure(IllegalStateException("file3")), Result.success("test4"), Result.success("root4"), Result.success("file4"), Result.failure(IllegalStateException("folder4")))
    val result = mTarget.parse("path/dotsave.json")
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("test1")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("root2", "file3", "folder4")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }
}
