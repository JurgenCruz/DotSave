package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import com.github.jurgencruz.dotsave.utils.toSafePath
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Handler for the restore process.
 */
@Suppress("HardCodedStringLiteral")
object RestoreHandler {
  /**
   * Restore files based on the configuration ofrom the specified path.
   * @param config The restore configuration.
   * @param configFilePath The path of the config file and directory to restore from.
   * @param profileName The profile to execute.
   * @param log The logger function.
   * @param copy function to copy a file from path a to path b.
   * @return A result object to signal if there were any errors.
   */
  fun restore(config: Config, configFilePath: String, profileName: String?, log: (LogLevel, String) -> Unit, copy: (Path, Path) -> Result<Unit>): Result<Unit> {
    val backupPath = Path(configFilePath).parent!!
    return config.profiles.asSequence().onEach {
      log(LogLevel.INFO, "Restoring profile: ${it.name}")
    }.flatMap { (name, _, root, _, _, include) ->
      include.asSequence().map { f ->
        toSafePath(root, f) to runCatching { backupPath.resolve(name).resolve(f) }
      }
    }.map { (root, f) ->
      root.flatMap { rootPath ->
        f.flatMap { filePath ->
          log(LogLevel.INFO, "Copying '$filePath' to '$rootPath'")
          copy(filePath, rootPath)
        }
      }
    }.mergeFailures().map { }
  }
}
