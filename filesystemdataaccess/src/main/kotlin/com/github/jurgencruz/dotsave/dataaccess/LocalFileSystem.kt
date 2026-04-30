package com.github.jurgencruz.dotsave.dataaccess

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

/**
 * File System implementation of the Data Access Layer.
 * @constructor Create a new File System object.
 */
object LocalFileSystem {
  /**
   * Read a file and return as string.
   * @param path The path of the file.
   * @return A result object with the contents of the file as string if successful or exception if error.
   */
  fun read(path: Path) = path.runCatching { toFile().readText(Charsets.UTF_8) }

  fun getFileSystem(dryRun: Boolean): FileSystem {
    return if (dryRun)
      FileSystem(
        ::exists,
        ::isDirectory,
        ::isFile,
        ::deleteDir,
        ::createDirectories,
        ::copyFile,
        ::walk
      )
    else
      FileSystem(
        ::exists,
        ::isDirectory,
        ::isFile,
        ::dryRunDeleteDir,
        ::dryRunCreateDirectories,
        ::dryRunCopyFile,
        ::walk
      )
  }

  private fun exists(path: Path) = path.exists()
  private fun isDirectory(path: Path) = path.isDirectory()
  private fun isFile(path: Path) = path.isRegularFile()
  private fun deleteDir(path: Path) = path.toFile().deleteRecursively()
  private fun dryRunDeleteDir(path: Path) = true
  private fun createDirectories(path: Path) {
    Files.createDirectories(path)
  }

  private fun dryRunCreateDirectories(path: Path) {}
  private fun copyFile(srcPath: Path, destPath: Path) = Files.copy(
    srcPath,
    destPath,
    LinkOption.NOFOLLOW_LINKS,
    StandardCopyOption.COPY_ATTRIBUTES,
    StandardCopyOption.REPLACE_EXISTING
  )

  private fun dryRunCopyFile(srcPath: Path, destPath: Path) {}
  private fun walk(path: Path, maxDepth: Int): Sequence<Path> = Files.walk(path, maxDepth).asSequence()
}
