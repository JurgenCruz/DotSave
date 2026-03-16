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
  fun backup(config: Config, configFilePath: String, profileName: String?, log: (LogLevel, String) -> Unit, recreateDir: (Path) -> Result<Unit>, copy: (Path, Path) -> Result<Unit>): Result<Unit> {
    val backupPath = Path(configFilePath).parent!!
    val profiles = config.profiles.map { profile ->
      runCatching { backupPath.resolve(profile.name) }.flatMap { profileBackupPath ->
        log(LogLevel.INFO, "Recreating directory: $profileBackupPath")
        recreateDir(profileBackupPath)
      }.map {
        profile
      }
    }.groupBy(Result<Profile>::isSuccess)
    val files = profiles.getOrElse(true, ::emptyList).asSequence().map {
      it.getOrNull()!!
    }.onEach {
      log(LogLevel.INFO, "Backing up profile: ${it.name}")
    }.flatMap { (name, _, root, _, _, include): Profile ->
      include.asSequence().map { f ->
        toSafePath(root, f) to runCatching { backupPath.resolve(name).resolve(f) }
      }.map { (f, profileBackupPath) ->
        f.flatMap { filePath ->
          profileBackupPath.flatMap { profileBackupPath ->
            log(LogLevel.INFO, "Copying '$filePath' to '$profileBackupPath'")
            copy(filePath, profileBackupPath)
          }
        }
      }
    }
    return profiles.getOrElse(false, ::emptyList).asSequence().map { it.map { } }.plus(files).mergeFailures().map { }
  }
}
