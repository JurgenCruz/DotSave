package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.logging.LogLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name

class BackupHandlerTest {
  val backupPath = Path("backup")
  val logStub = { _: LogLevel, _: String -> }
  val existsStub = { _: Path -> true }
  val isDirectoryStub = { path: Path -> !path.name.startsWith("file") }
  val isFileStub = { path: Path -> path.name.startsWith("file") }
  val deleteDirStub = { _: Path -> true }
  val createDirsStub = { _: Path -> }
  val copyStub = { _: Path, _: Path -> }
  val changeOwnerAndAttrsStub = { _: Path, _: FileMetaData -> }
  val getMetadataStub = { _: Path -> FileMetaData("owner", "r--------") }
  val readStub = { _: Path -> Result.success("") }
  val writeStub = { _: Path, _: String -> Result.success(Unit) }
  val walkStub = { p: Path, _: Int -> sequenceOf(p) }

  @Test
  fun backupShouldCopyFilesToCorrectDestination() {
    val config = Config(listOf(getProfile1(), getProfile2()))
    val copyList = mutableListOf<Pair<Path, Path>>()
    val copy: (Path, Path) -> Unit = { p1, p2 -> copyList.add(p1 to p2) }
    val fileSystem = FileSystem(
      existsStub,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirsStub,
      copy,
      changeOwnerAndAttrsStub,
      getMetadataStub,
      readStub,
      writeStub,
      walkStub
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
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
  fun backupShouldChangeOwnerAndPermissionsOfBackedUpFiles() {
    val config = Config(listOf(getProfile1(), getProfile2()))
    val copyList = mutableListOf<Pair<Path, FileMetaData>>()
    val changeOwnerAndAttrs: (Path, FileMetaData) -> Unit = { p, m -> copyList.add(p to m) }
    val walk = { a: Path, d: Int ->
      when (d) {
        1    -> sequenceOf(
          a,
          a.resolve("file1"),
          a.resolve("file2"),
        )

        else -> sequenceOf(a)
      }
    }
    val fileSystem = FileSystem(
      existsStub,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirsStub,
      copyStub,
      changeOwnerAndAttrs,
      getMetadataStub,
      readStub,
      writeStub,
      walk
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(copyList).hasSize(4)
    val expectedMetadata = FileMetaData("owner", "r--r--r--")
    assertThat(copyList).zipSatisfy(
      listOf(
        "backup/program1/file1" to expectedMetadata,
        "backup/program1/folder2" to expectedMetadata,
        "backup/program1/folder2/file1" to expectedMetadata,
        "backup/program1/folder2/file2" to expectedMetadata
      )
    ) { (srcPath, metadata), (expectedSrc, expectedMetadata) ->
      assertThat(srcPath).hasToString(expectedSrc)
      assertThat(metadata).isEqualTo(expectedMetadata)
    }
  }

  @Test
  fun backupShouldWriteFileMetadataOfBackedUpFiles() {
    val config = Config(listOf(getProfile1(), getProfile2()))
    var wp: Path? = null
    var ms: String? = null
    val write: (Path, String) -> Result<Unit> = { p, s ->
      wp = p
      ms = s
      Result.success(Unit)
    }
    val walk = { a: Path, d: Int ->
      when (d) {
        1    -> sequenceOf(
          a,
          a.resolve("file1"),
          a.resolve("file2"),
        )

        else -> sequenceOf(a)
      }
    }
    val fileSystem = FileSystem(
      existsStub,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirsStub,
      copyStub,
      changeOwnerAndAttrsStub,
      getMetadataStub,
      readStub,
      write,
      walk
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(wp).hasToString("backup/program1.json")
    assertThat(ms).isEqualTo("""{"/root1/file1":{"owner":"owner","permissions":"r--------"},"/root1/folder2/file1":{"owner":"owner","permissions":"r--------"},"/root1/folder2/file2":{"owner":"owner","permissions":"r--------"},"/root1/folder2":{"owner":"owner","permissions":"r--------"}}""")
  }

  @Test
  fun backupShouldFailIfCannotCreateDestinationDir() {
    val config = Config(listOf(getProfile1()))
    val exists = { _: Path -> false }
    val createDirectories = { _: Path -> throw IllegalAccessException("test") }
    val fileSystem = FileSystem(
      exists,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirectories,
      copyStub,
      changeOwnerAndAttrsStub,
      getMetadataStub,
      readStub,
      writeStub,
      walkStub
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }

  @Test
  fun backupShouldFailIfCannotCopyAFile() {
    val config = Config(listOf(getProfile1(), getProfile2()))
    val copy = { _: Path, _: Path -> throw IllegalAccessException("test") }
    val fileSystem = FileSystem(
      existsStub,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirsStub,
      copy,
      changeOwnerAndAttrsStub,
      getMetadataStub,
      readStub,
      writeStub,
      walkStub
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
    assertThat(result.exceptionOrNull()).hasMessage("test")
  }

  @Test
  fun backupShouldAggregateErrorsAndContinue() {
    val config = Config(listOf(Profile("program1", "/root1", listOf("file1", "folder2", "file3", "folder4"))))
    var i = 0
    val copy = { _: Path, _: Path -> throw IllegalAccessException("test${++i}") }
    val fileSystem = FileSystem(
      existsStub,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirsStub,
      copy,
      changeOwnerAndAttrsStub,
      getMetadataStub,
      readStub,
      writeStub,
      walkStub
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
    val exception = result.exceptionOrNull()
    assertThat(exception).hasMessage("test1")
    assertThat(exception!!.suppressed).zipSatisfy(arrayOf("test2", "test3", "test4")) { a, b ->
      assertThat(a).hasMessage(b)
    }
  }

  @Test
  fun backupShouldReturnListOfFilesNotBackedUpOrIgnored() {
    val config = Config(listOf(getProfile1()))
    val walk = { a: Path, d: Int ->
      when (d) {
        1    -> sequenceOf(
          a,
          a.resolve("file1"),
          a.resolve("file2"),
        )

        else -> sequenceOf(
          a,
          a.resolve("file1"),
          a.resolve("folder2/file1"),
          a.resolve("folder2/file2"),
          a.resolve("file3"),
          a.resolve("folder4/file1"),
          a.resolve("folder4/file2"),
          a.resolve("ignore1"),
          a.resolve("ignore2/file1"),
          a.resolve("ignore2/file2")
        )
      }
    }
    val fileSystem = FileSystem(
      existsStub,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirsStub,
      copyStub,
      changeOwnerAndAttrsStub,
      getMetadataStub,
      readStub,
      writeStub,
      walk
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().map(Path::toString)).containsExactly("/root1/file3", "/root1/folder4/file1", "/root1/folder4/file2")
  }

  @Test
  fun backupShouldRunIncludedProfiles() {
    val config = Config(
      listOf(
        Profile("program1", "/root", listOf("file1", "folder2"), includeProfiles = listOf("include1")),
        Profile("include1", "/root", listOf("file3", "folder4"), listOf("ignore1"), listOf("include2")),
        Profile("include2", "/root/sub", listOf("file5", "folder6"), listOf("ignore2"))
      )
    )
    val copyList = mutableListOf<Pair<Path, Path>>()
    val copy: (Path, Path) -> Unit = { p1, p2 -> copyList.add(p1 to p2) }
    val walk = { a: Path, _: Int ->
      when (a) {
        Path("/root")     -> sequenceOf(
          a,
          a.resolve("file1"),
          a.resolve("folder2/file1"),
          a.resolve("folder2/file2"),
          a.resolve("file3"),
          a.resolve("folder4/file1"),
          a.resolve("folder4/file2"),
          a.resolve("ignore1"),
          a.resolve("missing1"),
          a.resolve("sub/file5"),
          a.resolve("sub/folder6/file1"),
          a.resolve("sub/folder6/file2"),
          a.resolve("sub/ignore2/file1"),
          a.resolve("sub/ignore2/file2"),
        )

        Path("/root/sub") -> sequenceOf(
          a,
          a.resolve("file5"),
          a.resolve("folder6/file1"),
          a.resolve("folder6/file2"),
          a.resolve("ignore2/file1"),
          a.resolve("ignore2/file2"),
        )

        else              -> sequenceOf<Path>()
      }
    }
    val fileSystem = FileSystem(
      existsStub,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirsStub,
      copy,
      changeOwnerAndAttrsStub,
      getMetadataStub,
      readStub,
      writeStub,
      walk
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
    assertThat(result.exceptionOrNull()).isNull()
    assertThat(result.getOrThrow().map(Path::toString)).containsExactly("/root/missing1")
    assertThat(copyList).hasSize(6)
    assertThat(copyList).zipSatisfy(
      listOf(
        "/root/sub/file5" to "backup/include2/file5",
        "/root/sub/folder6" to "backup/include2/folder6",
        "/root/file3" to "backup/include1/file3",
        "/root/folder4" to "backup/include1/folder4",
        "/root/file1" to "backup/program1/file1",
        "/root/folder2" to "backup/program1/folder2"
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
    val copy: (Path, Path) -> Unit = { p1, p2 -> copyList.add(p1 to p2) }
    val walk = { a: Path, d: Int ->
      when (d) {
        1    -> sequenceOf(a)
        else -> sequenceOf(
          a,
          a.resolve("missing1"),
          a.resolve("missing2")
        )
      }
    }
    val fileSystem = FileSystem(
      existsStub,
      isDirectoryStub,
      isFileStub,
      deleteDirStub,
      createDirsStub,
      copy,
      changeOwnerAndAttrsStub,
      getMetadataStub,
      readStub,
      writeStub,
      walk
    )
    val result = BackupHandler.backup(config, config.profiles[0], backupPath, "owner", logStub, fileSystem)
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

  private fun getProfile1() = Profile("program1", "/root1", listOf("file1", "folder2"), listOf("ignore1", "ignore2"))
  private fun getProfile2() = Profile("program2", "/root2", listOf("file3", "folder4"))
}
