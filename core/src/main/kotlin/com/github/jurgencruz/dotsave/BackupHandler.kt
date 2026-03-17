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
   * @param configFilePath The path of the config file and directory to back up to.
   * @param profileName The profile to execute.
   * @param log The logger function.
   * @param recreateDir function that deletes and creates a directory again.
   * @param copy function to copy a file from path a to path b.
   * @return A result object to signal if there were any errors.
   */
  fun backup(config: Config, configFilePath: String, profileName: String?, log: (LogLevel, String) -> Unit, recreateDir: (Path) -> Result<Unit>, copy: (Path, Path) -> Result<Unit>, walk: (Path) -> Sequence<Path>): Result<List<String>> {
    val backupPath = Path(configFilePath).parent!!
    val profile = if (profileName.isNullOrBlank()) {
      config.profiles.firstOrNull { it.default } ?: return Result.failure(IllegalStateException("No default profile in config file and no profile name specified"))
    } else {
      config.profiles.firstOrNull { it.name == profileName } ?: return Result.failure(IllegalStateException("No profile with name: $profileName exists"))
    }
    return runProfile(config, profile, backupPath, log, recreateDir, copy, walk)
  }

  private fun runProfile(config: Config, profile: Profile, backupPath: Path, log: (LogLevel, String) -> Unit, recreateDir: (Path) -> Result<Unit>, copy: (Path, Path) -> Result<Unit>, walk: (Path) -> Sequence<Path>): Result<List<String>> = mergeProfile(config, profile).flatMap { p ->
    runIncludedProfiles(p, config, backupPath, log, recreateDir, copy, walk).map { p to it }
  }.flatMap { (p, missing) ->
    runCatching { backupPath.resolve(p.name) to (p to missing) }
  }.onSuccess { (path) ->
    log(LogLevel.INFO, "Recreating directory: $path")
  }.flatMap { (path, p) -> recreateDir(path).map { p } }.onSuccess { (p) ->
    log(LogLevel.INFO, "Backing up profile: ${p.name}")
  }.map {
    calculateMissingFiles(it.first, it.second, walk) to it.first
  }.flatMap { (missingFiles, profile) ->
    backupFiles(profile, backupPath, log, copy).map { missingFiles }
  }

  private fun calculateMissingFiles(profile: Profile, existingMissingFiles: List<String>, walk: (Path) -> Sequence<Path>) = walk(Path(profile.root)).map { "$it" }.toMutableSet().also { it.addAll(existingMissingFiles) }.filter { !profile.exclude.map { "${Path(profile.root, it)}" }.contains(it) }
  private fun backupFiles(profile: Profile, backupPath: Path, log: (LogLevel, String) -> Unit, copy: (Path, Path) -> Result<Unit>): Result<Unit> = profile.include.asSequence().map { f ->
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

  private fun runIncludedProfiles(profile: Profile, config: Config, backupPath: Path, log: (LogLevel, String) -> Unit, recreateDir: (Path) -> Result<Unit>, copy: (Path, Path) -> Result<Unit>, walk: (Path) -> Sequence<Path>) = profile.includeProfiles.asSequence().map { name ->
    runProfile(config, config.profiles.first { (n) -> n == name }, backupPath, log, recreateDir, copy, walk)
  }.mergeFailures().map { it.flatten() }

  private fun mergeProfile(config: Config, profile: Profile): Result<Profile> {
    return profile.inheritProfiles.fold(Result.success(profile)) { profile, toInheritName ->
      profile.flatMap { profile ->
        mergeProfile(config, config.profiles.first { (n) -> n == toInheritName }).map { it to profile }
      }.map { (toInherit, profile) ->
        val prefix = calculatePrefix(profile.root, toInherit.root)
        val newInclude = profile.include.toMutableSet()
        newInclude.addAll(toInherit.include.map { prefix.resolve(it).toString() })
        val newExclude = profile.exclude.toMutableSet()
        newExclude.addAll(toInherit.exclude.map { prefix.resolve(it).toString() })
        val newIncludeProfiles = profile.includeProfiles.toMutableSet()
        newIncludeProfiles.addAll(toInherit.includeProfiles)
        profile.copy(include = newInclude.toList(), exclude = newExclude.toList(), includeProfiles = newIncludeProfiles.toList())
      }
    }
  }

  private fun calculatePrefix(base: String, target: String) = Path(base).relativize(Path(target))
}
