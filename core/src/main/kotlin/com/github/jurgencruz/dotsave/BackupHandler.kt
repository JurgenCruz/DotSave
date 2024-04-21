package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.logging.Logger
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import com.github.jurgencruz.dotsave.utils.toSafePath
import kotlin.io.path.Path

/**
 * Handler for the backup process.
 * @constructor Create a new handler.
 * @param fileSystem The file system layer.
 * @param logger Th logger.
 */
@Suppress("HardCodedStringLiteral")
class BackupHandler(fileSystem: FileSystem, logger: Logger) {
  private val mFileSystem = fileSystem
  private val mLogger = logger

  /**
   * Backup files based on the configuration on the specified path.
   * @param config The backup configuration.
   * @param configFilePath The path of the config file and directory to back up to.
   * @return A result object to signal if there were any errors.
   */
  fun backup(config: Config, configFilePath: String): Result<Unit> {
    val backupPath = Path(configFilePath).parent!!
    val profiles = config.profiles.map { profile ->
      runCatching { backupPath.resolve(profile.name) }.flatMap { profileBackupPath ->
        mLogger.log("Recreating directory: $profileBackupPath")
        mFileSystem.recreateDir(profileBackupPath)
      }.map {
        profile
      }
    }.groupBy(Result<Profile>::isSuccess)
    val files = profiles.getOrElse(true, ::emptyList).asSequence().map {
      it.getOrNull()!!
    }.onEach {
      mLogger.log("Backing up profile: ${it.name}")
    }.flatMap { (name, root, include): Profile ->
      include.asSequence().map { f ->
        toSafePath(root, f) to runCatching { backupPath.resolve(name).resolve(f) }
      }.map { (f, profileBackupPath) ->
        f.flatMap { filePath ->
          profileBackupPath.flatMap { profileBackupPath ->
            mLogger.log("Copying '$filePath' to '$profileBackupPath'")
            mFileSystem.copy(filePath, profileBackupPath)
          }
        }
      }
    }
    return profiles.getOrElse(false, ::emptyList).asSequence().map { it.map { } }.plus(files).mergeFailures()
  }
}
