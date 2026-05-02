package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.utils.deserialize
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import java.nio.file.Path

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
  ) = fileSystem.read(backupPath.resolve("${profile.name}.json")).flatMap {
    deserialize<Map<String, FileMetaData>>(it)
  }.flatMap { metaDatas ->
    val bPath = backupPath.resolve(profile.name)
    profile.includePath.asSequence().map { inc ->
      copy(bPath, profile.rootPath, inc, metaDatas, fileSystem, log)
    }.mergeFailures().map { }
  }

  private fun copy(
    backupPath: Path,
    rootPath: Path,
    relativePath: Path,
    metaDatas: Map<String, FileMetaData>,
    fileSystem: FileSystem,
    log: (LogLevel, String) -> Unit
  ): Result<Unit> {
    val srcPath = backupPath.resolve(relativePath)
    val destPath = rootPath.resolve(relativePath)
    log(LogLevel.INFO, "Copying '$srcPath' to '$destPath'")
    if (!fileSystem.exists(srcPath)) {
      return Result.failure(IllegalStateException("Path '$srcPath' does not exist, cannot copy to '$destPath'."))
    }
    return if (fileSystem.isFile(srcPath)) {
      runCatching {
        createParentDirs(srcPath.parent, destPath.parent, fileSystem, metaDatas, log)
        fileSystem.copyFile(srcPath, destPath)
        val metaData = metaDatas["$destPath"] ?: throw IllegalStateException("No metadata for '$destPath'")
        log(LogLevel.INFO, "Changing '$destPath's owner to ${metaData.owner} and permissions to ${metaData.permissions}.")
        fileSystem.changeOwnerAndAttrs(destPath, metaData)
      }
    } else {
      runCatching {
        createParentDirs(srcPath.parent, destPath.parent, fileSystem, metaDatas, log)
        fileSystem.copyFile(srcPath, destPath)
        val metaData = metaDatas["$destPath"] ?: throw IllegalStateException("No metadata for '$destPath'")
        log(LogLevel.INFO, "Changing '$destPath's owner to ${metaData.owner} and permissions to ${metaData.permissions}.")
        fileSystem.changeOwnerAndAttrs(destPath, metaData)
        fileSystem.walk(srcPath, 1)
      }.flatMap { files ->
        files.drop(1).map { walkFile ->
          copy(srcPath, destPath, walkFile.fileName, metaDatas, fileSystem, log)
        }.mergeFailures().map { }
      }
    }
  }

  private fun createParentDirs(
    srcPath: Path?,
    destPath: Path?,
    fileSystem: FileSystem,
    metaDatas: Map<String, FileMetaData>,
    log: (LogLevel, String) -> Unit
  ) {
    if (destPath != null && srcPath != null && !fileSystem.exists(destPath)) {
      createParentDirs(srcPath.parent, destPath.parent, fileSystem, metaDatas, log)
      fileSystem.copyFile(srcPath, destPath)
      val metaData = metaDatas["$destPath"] ?: throw IllegalStateException("No metadata for '$destPath'")
      log(LogLevel.INFO, "Changing '$destPath's owner to ${metaData.owner} and permissions to ${metaData.permissions}.")
      fileSystem.changeOwnerAndAttrs(destPath, metaData)
    }
  }
}
