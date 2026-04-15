package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import com.github.jurgencruz.dotsave.utils.toSafePath
import java.nio.file.Path
import kotlin.io.path.Path

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
  ): Result<List<String>> = Profile.mergeProfile(config, profile).flatMap { p ->
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
    existingMissingFiles: List<String>,
    walk: (Path) -> Sequence<Path>
  ) = walk(Path(profile.root)).map { "$it" }.toMutableSet().also { it.addAll(existingMissingFiles) }.filter {
    notIncludedOrIgnored(profile, it)
  }

  private fun notIncludedOrIgnored(profile: Profile, file: String): Boolean {
    val (ignoredFiles, ignoredDirs) = getFilesAndDirs(profile.ignore, profile)
    val (includedFiles, includedDirs) = getFilesAndDirs(profile.include, profile)
    return !(ignoredFiles.contains(file)
        || ignoredDirs.any { file.startsWith(it) }
        || includedFiles.contains(file)
        || includedDirs.any { file.startsWith(it) })
  }

  private fun getFilesAndDirs(list: List<String>, profile: Profile): Pair<List<String>, List<String>> {
    val files = mutableListOf<String>()
    val dirs = mutableListOf<String>()
    list.forEach {
      if (it.endsWith("/")) {
        dirs.add("${Path(profile.root, it)}/")
      } else {
        files.add("${Path(profile.root, it)}")
      }
    }
    return files to dirs
  }

  private fun backupFiles(
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    copy: (Path, Path) -> Result<Unit>
  ): Result<Unit> = profile.include.asSequence().map { f ->
    toSafePath(profile.root, f).flatMap { path ->
      runCatching {
        path to backupPath.resolve(profile.name).resolve(f)
      }
    }.onSuccess { (filePath, profileBackupPath) ->
      log(LogLevel.INFO, "Copying '$filePath' to '$profileBackupPath'")
    }.flatMap { (filePath, profileBackupPath) ->
      copy(filePath, profileBackupPath)
    }
  }.mergeFailures().map { }

  private fun runIncludedProfiles(
    profile: Profile,
    config: Config,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    recreateDir: (Path) -> Result<Unit>,
    copy: (Path, Path) -> Result<Unit>,
    walk: (Path) -> Sequence<Path>
  ) = profile.includeProfiles.asSequence().map { name ->
    backup(config, config.profiles.first { (n) -> n == name }, backupPath, log, recreateDir, copy, walk)
  }.mergeFailures().map { it.flatten() }
}
