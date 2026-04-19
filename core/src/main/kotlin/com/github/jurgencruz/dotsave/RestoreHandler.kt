package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.logging.LogLevel
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
  ) = profile.mergeInheritedProfiles(config).flatMap { p ->
    runIncludedProfiles(p, config, backupPath, log, copy).map { p }
  }.onSuccess { p ->
    log(LogLevel.INFO, "Restoring up profile: ${p.name}")
  }.flatMap { p ->
    restoreFiles(p, backupPath, log, copy)
  }

  private fun restoreFiles(
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    copy: (Path, Path) -> Result<Unit>
  ) = backupPath.resolve(profile.name).let { bPath ->
    profile.includePath.asSequence().map { inc ->
      val srcPath = bPath.resolve(inc)
      val desPath = profile.rootPath.resolve(inc)
      log(LogLevel.INFO, "Copying '$srcPath' to '$desPath'")
      copy(srcPath, desPath)
    }.mergeFailures().map { }
  }

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
