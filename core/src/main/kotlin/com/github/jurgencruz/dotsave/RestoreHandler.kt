package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Handler for the restore process.
 */
@Suppress("HardCodedStringLiteral")
object RestoreHandler {
  /**
   * Restore files based on the configuration from the specified path.
   * @param config The restore configuration.
   * @param profile The profile to execute.
   * @param backupPath The path of the directory to restore from.
   * @param log The logger function.
   * @param fileSystem The group of functions to interact with the File System.
   * @return A result object to signal if there were any errors.
   */
  fun restore(
    config: Config,
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ) = profile.mergeInheritedProfiles(config).flatMap { p ->
    runIncludedProfiles(p, config, backupPath, log, fileSystem).map { p }
  }.onSuccess { p ->
    log(LogLevel.INFO, "Restoring up profile: ${p.name}")
  }.flatMap { p ->
    restoreFiles(p, backupPath, log, fileSystem)
  }

  private fun runIncludedProfiles(
    profile: Profile,
    config: Config,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ): Result<Unit> = profile.includeProfiles.asSequence().map { name ->
    restore(config, config.profiles.first { (n) -> n == name }, backupPath, log, fileSystem)
  }.mergeFailures().map { }

  private fun restoreFiles(
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ) = backupPath.resolve(profile.name).let { bPath ->
    profile.includePath.asSequence().map { inc ->
      val srcPath = bPath.resolve(inc)
      val desPath = profile.rootPath.resolve(inc)
      log(LogLevel.INFO, "Copying '$srcPath' to '$desPath'")
      copy(srcPath, desPath, fileSystem)
    }.mergeFailures().map { }
  }

  private fun copy(srcPath: Path, destPath: Path, fileSystem: FileSystem): Result<Unit> {
    if (!fileSystem.exists(srcPath)) {
      return Result.failure(IllegalStateException("Path '$srcPath' does not exist, cannot copy to '$destPath'."))
    }
    return if (fileSystem.isFile(srcPath)) {
      runCatching {
        fileSystem.createParentDirs(srcPath.parent, destPath.parent)
        fileSystem.copyFile(srcPath, destPath)
      }
    } else {
      runCatching {
        fileSystem.createParentDirs(srcPath.parent, destPath.parent)
        fileSystem.copyFile(srcPath, destPath)
        fileSystem.walk(srcPath, 1)
      }.flatMap { files ->
        files.drop(1).map { srcFile ->
          copy(srcFile, destPath.resolve(srcFile.name), fileSystem)
        }.mergeFailures().map { }
      }
    }
  }
}
