package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import com.github.jurgencruz.dotsave.utils.toSafePath
import java.nio.file.Path

/**
 * Handler for the restore process.
 */
@Suppress("HardCodedStringLiteral")
object RestoreHandler {
  /**
   * Restore files based on the configuration from the specified path.
   * @param config The restore configuration.
   * @param backupPath The path of the directory to restore from.
   * @param profile The profile to execute.
   * @param log The logger function.
   * @param copy function to copy a file from path a to path b.
   * @return A result object to signal if there were any errors.
   */
  fun restore(
    config: Config,
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    copy: (Path, Path) -> Result<Unit>
  ): Result<Unit> {
    return Profile.mergeProfile(config, profile).flatMap { p ->
      runIncludedProfiles(p, config, backupPath, log, copy).map { p }
    }.onSuccess { p ->
      log(LogLevel.INFO, "Restoring up profile: ${p.name}")
    }.flatMap { p ->
      restoreFiles(p, backupPath, log, copy)
    }
  }

  private fun restoreFiles(
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    copy: (Path, Path) -> Result<Unit>
  ) = profile.include.asSequence().map { f ->
    toSafePath(profile.root, f).flatMap { path ->
      runCatching {
        path to backupPath.resolve(profile.name).resolve(f)
      }
    }.onSuccess { (filePath, profileBackupPath) ->
      log(LogLevel.INFO, "Copying '$profileBackupPath' to '$filePath'")
    }.flatMap { (filePath, profileBackupPath) ->
      copy(profileBackupPath, filePath)
    }
  }.mergeFailures().map { }

  private fun runIncludedProfiles(
    profile: Profile,
    config: Config,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    copy: (Path, Path) -> Result<Unit>
  ): Result<Unit> = profile.includeProfiles.asSequence().map { name ->
    restore(config, config.profiles.first { (n) -> n == name }, backupPath, log, copy)
  }.mergeFailures().map { }
}
