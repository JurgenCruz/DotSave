package com.github.jurgencruz.dotsave.dataaccess

import com.github.jurgencruz.dotsave.FileMetaData
import java.nio.file.Path

/**
 * Represents a filesystem abstraction providing operations to check existence,
 * file and directory status, deletion, creation of directories, copying of files,
 * changing metadata including ownership and attributes, retrieving metadata,
 * reading from and writing to files.
 *
 * @property exists Checks if the given path exists in the filesystem.
 * @property isDirectory Determines if the given path is a directory.
 * @property isFile Determines if the given path is a file.
 * @property deleteDir Deletes the directory at the specified path.
 * @property createDirectories Creates all directories necessary to make the given path valid.
 * @property copyFile Copies a file from one location to another.
 * @property changeOwnerAndAttrs Changes the owner and attributes of the specified path using provided metadata.
 * @property getMetadata Retrieves metadata for the specified path.
 * @property read Reads the content of the specified file, returning a Result with the string content or an error if reading fails.
 * @property write Writes content to the specified file, returning a Result indicating success or failure.
 */
data class FileSystem(
  val exists: (Path) -> Boolean,
  val isDirectory: (Path) -> Boolean,
  val isFile: (Path) -> Boolean,
  val deleteDir: (Path) -> Boolean,
  val createDirectories: (Path) -> Unit,
  val copyFile: (Path, Path) -> Unit,
  val changeOwnerAndAttrs: (Path, FileMetaData) -> Unit,
  val getMetadata: (Path) -> FileMetaData,
  val read: (Path) -> Result<String>,
  val write: (Path, String) -> Result<Unit>,
  private val mWalk: (Path, Int) -> Sequence<Path>
) {
  /**
   * Recursively creates parent directories for the destination path and copies the source file there if it doesn't exist.
   *
   * @param srcPath The source dir that we are using as reference.
   * @param destPath The destination dir that we are recreating the tree for.
   */
  fun createParentDirs(srcPath: Path?, destPath: Path?) {
    if (destPath != null && srcPath != null && !exists(destPath)) {
      createParentDirs(srcPath.parent, destPath.parent)
      copyFile(srcPath, destPath)
    }
  }

  /**
   * Walks down through the filesystem starting from the specified path up to a maximum depth.
   *
   * @param path The path to walk down.
   * @param maxDepth The maximum depth to walk down allowed.
   * @return A sequence of paths (including the root dir) found inside the dir.
   */
  fun walk(path: Path, maxDepth: Int = Int.MAX_VALUE) = mWalk(path, maxDepth)
}