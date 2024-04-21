package com.github.jurgencruz.dotsave.dataaccess

import java.nio.file.Path

/**
 * The Data Access Layer.
 */
interface FileSystem {
  /**
   * Read a file and return as string.
   * @param path The path of the file.
   * @return A result object with the contents of the file as string if successful or exception if error.
   */
  fun read(path: Path): Result<String>

  /**
   * Delete and create a directory again to start fresh.
   * @param path The path of the directory to recreate.
   * @return A result object to signal if the operation was successful.
   */
  fun recreateDir(path: Path): Result<Unit>

  /**
   * Copy a file or directory to the destination directory.
   * @param srcPath The path of the file or directory to copy.
   * @param destPath The path of the destination file or directory to copy to.
   * @return A result object to signal if the operation was successful.
   */
  fun copy(srcPath: Path, destPath: Path): Result<Unit>
}
