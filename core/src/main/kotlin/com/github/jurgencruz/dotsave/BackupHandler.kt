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
 * Handler for the backup process.
 */
@Suppress("HardCodedStringLiteral")
object BackupHandler {
  /**
   * Backup files based on the configuration on the specified path.
   * @param config The backup configuration.
   * @param profile The profile to execute.
   * @param backupPath The path of the directory to back up to.
   * @param log The logger function.
   * @param fileSystem The group of functions to interact with the File System.
   * @return A result object with the list of missing files if successful or exception if failure.
   */
  fun backup(
    config: Config,
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ) = backupWithLists(config, profile, backupPath, log, fileSystem).map { (roots, copiedPaths, ignoredPaths) ->
    calculateMissingFiles(roots, copiedPaths, ignoredPaths, fileSystem)
  }

  private fun backupWithLists(
    config: Config,
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ): Result<Triple<MutableList<Path>, MutableSet<Path>, MutableSet<Path>>> = profile.mergeInheritedProfiles(config).flatMap { p ->
    runIncludedProfiles(p, config, backupPath, log, fileSystem).map { p to it }
  }.flatMap { (p, lists) ->
    runCatching { backupPath.resolve(p.name) to (p to lists) }
  }.onSuccess { (path) ->
    log(LogLevel.INFO, "Recreating directory: $path")
  }.flatMap { (path, p) ->
    recreateDir(path, fileSystem).map { p }
  }.onSuccess { (p, _) ->
    log(LogLevel.INFO, "Backing up profile: ${p.name}")
  }.flatMap { (profile, lists) ->
    backupFiles(profile, backupPath, log, fileSystem).map { new ->
      calculateRootsAndFiles(
        lists.first,
        lists.second,
        lists.third,
        listOf(profile.rootPath),
        new,
        profile.ignorePath.map { profile.rootPath.resolve(it) },
        log
      )
      lists
    }
  }

  private fun runIncludedProfiles(
    profile: Profile,
    config: Config,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ): Result<Triple<MutableList<Path>, MutableSet<Path>, MutableSet<Path>>> = profile.includeProfiles.asSequence().map { name ->
    backupWithLists(config, config.profiles.first { (n) -> n == name }, backupPath, log, fileSystem)
  }.mergeFailures().map { list ->
    val roots = mutableListOf<Path>()
    val copiedPaths = mutableSetOf<Path>()
    val ignoredPaths = mutableSetOf<Path>()
    list.forEach { (r, c, i) ->
      calculateRootsAndFiles(roots, copiedPaths, ignoredPaths, r, c.toList(), i.toList(), log)
    }
    Triple(roots, copiedPaths, ignoredPaths)
  }

  private fun calculateRootsAndFiles(
    roots: MutableList<Path>,
    copiedPaths: MutableSet<Path>,
    ignoredPaths: MutableSet<Path>,
    newRoots: List<Path>,
    newCopiedPaths: List<Path>,
    newIgnoredPaths: List<Path>,
    log: (LogLevel, String) -> Unit
  ) {
    newRoots.forEach { newRoot ->
      if (!roots.any { newRoot.startsWith(it) }) {
        roots.add(newRoot)
      }
    }
    newCopiedPaths.forEach {
      if (!copiedPaths.add(it)) {
        log(LogLevel.ERROR, "File '$it' is duplicated in the backup.")
      }
    }
    ignoredPaths.addAll(newIgnoredPaths)
  }

  private fun recreateDir(path: Path, fileSystem: FileSystem): Result<Unit> {
    if (fileSystem.exists(path)) {
      if (!fileSystem.isDirectory(path)) {
        return Result.failure(IllegalStateException("Path '$path' is not a directory, cannot recreate."))
      }
      if (!fileSystem.deleteDir(path)) {
        return Result.failure(IllegalStateException("Could not delete directory."))
      }
    }
    return runCatching { fileSystem.createDirectories(path) }
  }

  private fun backupFiles(
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ) = backupPath.resolve(profile.name).let { bPath ->
    profile.includePath.asSequence().map { inc ->
      val srcPath = profile.rootPath.resolve(inc)
      val desPath = bPath.resolve(inc)
      log(LogLevel.INFO, "Copying '$srcPath' to '$desPath'")
      copy(srcPath, desPath, fileSystem).map { srcPath }
    }.mergeFailures()
  }

  private fun copy(srcPath: Path, destPath: Path, fileSystem: FileSystem): Result<Unit> {
    if (!fileSystem.exists(srcPath)) {
      return Result.failure(IllegalStateException("Path '$srcPath' does not exist, cannot copy to '$destPath'."))
    }
    return if (fileSystem.isFile(srcPath)) {
      runCatching {
        fileSystem.createParentDirs(destPath.parent, srcPath.parent)
        fileSystem.copyFile(srcPath, destPath)
      }
    } else {
      runCatching {
        fileSystem.createParentDirs(destPath.parent, srcPath.parent)
        fileSystem.copyFile(srcPath, destPath)
        fileSystem.walk(srcPath, 1)
      }.flatMap { files ->
        files.drop(1).map { srcFile ->
          copy(srcFile, destPath.resolve(srcFile.name), fileSystem)
        }.mergeFailures().map { }
      }
    }
  }

  private fun calculateMissingFiles(
    roots: MutableList<Path>,
    copiedPaths: MutableSet<Path>,
    ignoredPaths: MutableSet<Path>,
    fileSystem: FileSystem
  ) = roots.flatMap {
    fileSystem.walk(it).drop(1).toList()
  }.filter {
    notIncludedOrIgnored(copiedPaths, ignoredPaths, it)
  }

  private fun notIncludedOrIgnored(copiedPaths: MutableSet<Path>, ignoredPaths: MutableSet<Path>, file: Path) =
    !(ignoredPaths.any { file.startsWith(it) } || copiedPaths.any { file.startsWith(it) })
}
