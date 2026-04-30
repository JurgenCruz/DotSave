package com.github.jurgencruz.dotsave.dataaccess

import java.nio.file.Path

data class FileSystem(
  val exists: (Path) -> Boolean,
  val isDirectory: (Path) -> Boolean,
  val isFile: (Path) -> Boolean,
  val deleteDir: (Path) -> Boolean,
  val createDirectories: (Path) -> Unit,
  val copyFile: (Path, Path) -> Unit,
  private val mWalk: (Path, Int) -> Sequence<Path>
) {
  fun createParentDirs(destPath: Path?, srcPath: Path?) {
    if (destPath != null && srcPath != null && !exists(destPath)) {
      createParentDirs(destPath.parent, srcPath.parent)
      copyFile(srcPath, destPath)
    }
  }

  fun walk(path: Path, maxDepth: Int = Int.MAX_VALUE) = mWalk(path, maxDepth)
}