package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import java.nio.file.Path

/**
 * Handler for the backup process.
 */
@Suppress("HardCodedStringLiteral")
object BackupHandler {
  /**
   * Backup files based on the configuration on the specified path.
   * @param config The backup configuration.
   * @param backupPath The path of the directory to back up to.
   * @param profile The profile to execute.
   * @param log The logger function.
   * @param recreateDir function that deletes and creates a directory again.
   * @param copy function to copy a file from path a to path b.
   * @return A result object with the list of missing files if successful or exception if failure.
   */
  fun backup(
    config: Config,
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    recreateDir: (Path) -> Result<Unit>,
    copy: (Path, Path) -> Result<Unit>,
    walk: (Path) -> Sequence<Path>
  ) = profile.mergeInheritedProfiles(config).flatMap { p ->
    runIncludedProfiles(p, config, backupPath, log, recreateDir, copy, walk).map { p to it }
  }.flatMap { (p, missing) ->
    runCatching { backupPath.resolve(p.name) to (p to missing) }
  }.onSuccess { (path) ->
    log(LogLevel.INFO, "Recreating directory: $path")
  }.flatMap { (path, p) ->
    recreateDir(path).map { p }
  }.onSuccess { (p, _) ->
    log(LogLevel.INFO, "Backing up profile: ${p.name}")
  }.map { (profile, missingFiles) ->
    calculateMissingFiles(profile, missingFiles, walk) to profile
  }.flatMap { (missingFiles, profile) ->
    backupFiles(profile, backupPath, log, copy).map { missingFiles }
  }

  private fun calculateMissingFiles(
    profile: Profile,
    existingMissingFiles: List<Path>,
    walk: (Path) -> Sequence<Path>
  ) = walk(profile.rootPath).toMutableSet().also { it.addAll(existingMissingFiles) }.filter {
    notIncludedOrIgnored(profile, it)
  }

  private fun notIncludedOrIgnored(profile: Profile, file: Path) =
    !(isFileInList(profile.ignorePath, file, profile) || isFileInList(profile.includePath, file, profile))

  private fun isFileInList(list: List<Path>, file: Path, profile: Profile): Boolean =
    list.any {
      file.startsWith(profile.rootPath.resolve(it))
    }

  private fun backupFiles(
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    copy: (Path, Path) -> Result<Unit>
  ) = backupPath.resolve(profile.name).let { bPath ->
    profile.includePath.asSequence().map { inc ->
      val srcPath = profile.rootPath.resolve(inc)
      val desPath = bPath.resolve(inc)
      log(LogLevel.INFO, "Copying '$srcPath' to '$desPath'")
      copy(srcPath, desPath)
    }.mergeFailures().map { }
  }

  private fun runIncludedProfiles(
    profile: Profile,
    config: Config,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    recreateDir: (Path) -> Result<Unit>,
    copy: (Path, Path) -> Result<Unit>,
    walk: (Path) -> Sequence<Path>
  ): Result<List<Path>> = profile.includeProfiles.asSequence().map { name ->
    backup(config, config.profiles.first { (n) -> n == name }, backupPath, log, recreateDir, copy, walk)
  }.mergeFailures().map { it.flatten() }
}
