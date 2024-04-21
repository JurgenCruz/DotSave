package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.logging.Logger
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import com.github.jurgencruz.dotsave.utils.toSafePath
import kotlin.io.path.Path

/**
 * Handler for the restore process.
 * @constructor Create a new handler.
 * @param fileSystem The file system layer.
 * @param logger Th logger.
 */
@Suppress("HardCodedStringLiteral")
class RestoreHandler(fileSystem: FileSystem, logger: Logger) {
  private val mFileSystem = fileSystem
  private val mLogger = logger

  /**
   * Restore files based on the configuration ofrom the specified path.
   * @param config The restore configuration.
   * @param configFilePath The path of the config file and directory to restore from.
   * @return A result object to signal if there were any errors.
   */
  fun restore(config: Config, configFilePath: String): Result<Unit> {
    val backupPath = Path(configFilePath).parent!!
    return config.profiles.asSequence().onEach {
      mLogger.log("Restoring profile: ${it.name}")
    }.flatMap { (name, root, include) ->
      include.asSequence().map { f ->
        toSafePath(root, f) to runCatching { backupPath.resolve(name).resolve(f) }
      }
    }.map { (root, f) ->
      root.flatMap { rootPath ->
        f.flatMap { filePath ->
          mLogger.log("Copying '$filePath' to '$rootPath'")
          mFileSystem.copy(filePath, rootPath)
        }
      }
    }.mergeFailures()
  }
}
