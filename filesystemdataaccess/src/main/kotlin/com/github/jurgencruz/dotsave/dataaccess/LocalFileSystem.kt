package com.github.jurgencruz.dotsave.dataaccess

import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
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
  fun read(path: Path): Result<String> {
    return if (path.exists()) {
      path.toFile().runCatching { readText(Charsets.UTF_8) }
    } else {
      Result.failure(FileNotFoundException("file not found: $path"))
    }
  }
  /**
   * Delete and create a directory again to start fresh.
   * @param path The path of the directory to recreate.
   * @return A result object to signal if the operation was successful.
   */
  fun recreateDir(path: Path): Result<Unit> {
    if (path.exists()) {
      if (!path.isDirectory()) {
        return Result.failure(IllegalStateException("Path '$path' is not a directory, cannot recreate."))
      }
      if (!path.toFile().deleteRecursively()) {
        return Result.failure(IllegalStateException("Could not delete directory."))
      }
    }
    return runCatching { Files.createDirectories(path) }
  }
  /**
   * Copy a file or directory to the destination directory.
   * @param srcPath The path of the file or directory to copy.
   * @param destPath The path of the destination file or directory to copy to.
   * @return A result object to signal if the operation was successful.
   */
  fun copy(srcPath: Path, destPath: Path): Result<Unit> {
    if (!srcPath.exists()) {
      return Result.failure(IllegalStateException("Path '$srcPath' does not exist, cannot copy to '$destPath'."))
    }
    return if (srcPath.isRegularFile()) {
      runCatching {
        Files.copy(srcPath, destPath, LinkOption.NOFOLLOW_LINKS, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
        Unit
      }
    } else {
      runCatching {
        Files.copy(srcPath, destPath, LinkOption.NOFOLLOW_LINKS, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
        Files.walk(srcPath, 1)
      }.flatMap { files ->
        files.asSequence().drop(1).map { srcFile ->
          copy(srcFile, destPath.resolve(srcFile.name))
        }.mergeFailures().map { }
      }
    }
  }
}
