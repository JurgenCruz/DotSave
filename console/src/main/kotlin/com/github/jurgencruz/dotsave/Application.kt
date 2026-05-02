package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.EnvVarReplacer
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.dataaccess.LocalFileSystem
import com.github.jurgencruz.dotsave.logging.ConsoleLogger
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.parse.Action
import com.github.jurgencruz.dotsave.parse.ArgsParser
import com.github.jurgencruz.dotsave.utils.deserialize
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.toSafePath
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.system.exitProcess

/**
 * Entry class for Application.
 */
@Suppress("HardCodedStringLiteral")
object Application {
  /**
   * Entry point for Application.
   */
  @JvmStatic
  fun main(args: Array<String>) {
    ArgsParser.parse(args).fold({ (action, configFilePath, verbose, profileName, dryRun, owner) ->
      val log = if (verbose) ConsoleLogger::verboseLog else ConsoleLogger::log
      when (action) {
        Action.USAGE   -> printUsage()
        Action.VERSION -> printVersion()
        Action.BACKUP  -> {
          printFlags(verbose, dryRun, action, configFilePath, profileName, owner)
          backup(configFilePath, profileName, owner ?: System.getProperty("user.name"), log, dryRun)
        }

        Action.RESTORE -> {
          printFlags(verbose, dryRun, action, configFilePath, profileName, owner)
          restore(configFilePath, profileName, log, dryRun)
        }
      }
    }, {
      ConsoleLogger.printErrors(it)
      printUsage()
      exitProcess(1)
    })
  }

  private fun backup(configFilePath: String, profileName: String?, owner: String, log: (LogLevel, String) -> Unit, dryRun: Boolean) {
    getContext(configFilePath, profileName, dryRun).flatMap { (path, config, profile, fileSystem) ->
      BackupHandler.backup(config, profile, path, owner, log, fileSystem)
    }.fold({
      printMissingFiles(it)
    }, {
      ConsoleLogger.printErrors(it)
      exitProcess(2)
    })
  }

  private fun restore(configFilePath: String, profileName: String?, log: (LogLevel, String) -> Unit, dryRun: Boolean) {
    getContext(configFilePath, profileName, dryRun).flatMap { (path, config, profile, fileSystem) ->
      RestoreHandler.restore(config, profile, path, log, fileSystem)
    }.onFailure {
      ConsoleLogger.printErrors(it)
      exitProcess(3)
    }
  }

  private fun getContext(configFilePath: String, profileName: String?, dryRun: Boolean) = toSafePath(configFilePath).flatMap { path ->
    val fileSystem = LocalFileSystem.getFileSystem(dryRun)
    getConfig(path, fileSystem).map { Triple(path, it, fileSystem) }
  }.flatMap { (path, config, fileSystem) ->
    config.selectProfile(profileName).map { profile -> Context(path.absolute().parent, config, profile, fileSystem) }
  }

  private fun getConfig(path: Path, fileSystem: FileSystem) = readConfig(path, fileSystem)
    .flatMap(EnvVarReplacer::replaceEnvVars)
    .mapCatching { it.apply { validate() } }

  private fun readConfig(path: Path, fileSystem: FileSystem) = fileSystem.read(path).flatMap<Config, String>(::deserialize)
  private fun printMissingFiles(missingFiles: List<Path>) {
    if (missingFiles.isEmpty()) {
      println("No files found that were not marked up for backup or ignored.")
    } else {
      println("The following files were found under the profile root folder and not marked up for backup or ignored. This could mean they are new files recently added. Please review and adjust your config:")
      missingFiles.forEach { println("  - $it") }
    }
  }

  private fun printFlags(verbose: Boolean, dryRun: Boolean, action: Action, configFilePath: String, profileName: String?, owner: String?) {
    if (verbose) {
      println("verbose mode: On")
      println("dry run mode: ${if (dryRun) "On" else "Off"}")
      println("action: $action")
      println("config path: $configFilePath")
      println("profile: ${profileName ?: "default"}")
      println("owner: ${owner ?: "current user"}")
    }
  }

  private fun printUsage() {
    println("Usage:")
    println("    dotsave  <operation>")
    println("Operations:")
    println("    {-h, --help}")
    println("    {-V, --version}")
    println("    {-b, --back-up <filepath>} [options]")
    println("    {-r, --restore <filepath>} [options]")
    println("Options:")
    println("    -b, --back-up <filepath> Back up the files according to the config file.")
    println("    -r, --restore <filepath> Restore the files according to the config file.")
    println("    -v, --verbose            Print extra information.")
    println("    -d, --dry-run            Simulate operation without actually touching the filesystem")
    println("    -p, --profile            Select which profile to execute.")
    println("    -o, --owner              Set the owner of the backed-up files (default: current user).")
  }

  private fun printVersion() {
    println("dotsave v 1.0.0")
  }
}
