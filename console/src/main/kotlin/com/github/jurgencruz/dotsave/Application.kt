package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.EnvVarReplacer
import com.github.jurgencruz.dotsave.dataaccess.LocalFileSystem
import com.github.jurgencruz.dotsave.logging.ConsoleLogger
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.parse.Action
import com.github.jurgencruz.dotsave.parse.ArgsParser
import com.github.jurgencruz.dotsave.utils.deserialize
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.toSafePath
import java.nio.file.Path
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
    ArgsParser.parse(args).fold({ (action, configFilePath, verbose, profileName, dryRun) ->
      val log = if (verbose) ConsoleLogger::verboseLog else ConsoleLogger::log
      when (action) {
        Action.USAGE   -> printUsage()
        Action.VERSION -> printVersion()
        Action.BACKUP  -> backup(configFilePath, profileName, log, dryRun)
        Action.RESTORE -> restore(configFilePath, profileName, log, dryRun)
      }
    }, {
      ConsoleLogger.printErrors(it)
      printUsage()
      exitProcess(1)
    })
  }

  private fun backup(configFilePath: String, profileName: String?, log: (LogLevel, String) -> Unit, dryRun: Boolean) {
    getContext(configFilePath, profileName).flatMap { (path, config, profile) ->
      BackupHandler.backup(
        config,
        profile,
        path,
        log,
        if (dryRun) ::dryRunRecreateDir else LocalFileSystem::recreateDir,
        if (dryRun) ::dryRunCopy else LocalFileSystem::copy,
        LocalFileSystem::walk
      )
    }.fold({
      printMissingFiles(it)
    }, {
      ConsoleLogger.printErrors(it)
      exitProcess(2)
    })
  }

  private fun restore(configFilePath: String, profileName: String?, log: (LogLevel, String) -> Unit, dryRun: Boolean) {
    getContext(configFilePath, profileName).flatMap { (path, config, profile) ->
      RestoreHandler.restore(config, profile, path, log, if (dryRun) ::dryRunCopy else LocalFileSystem::copy)
    }.onFailure {
      ConsoleLogger.printErrors(it)
      exitProcess(3)
    }
  }

  private fun getContext(configFilePath: String, profileName: String?) = toSafePath(configFilePath).flatMap { path ->
    getConfig(path).map { path to it }
  }.flatMap { (path, config) ->
    config.selectProfile(profileName).map { profile -> Context(path.parent!!, config, profile) }
  }

  private fun getConfig(path: Path) = readConfig(path).flatMap(EnvVarReplacer::replaceEnvVars).mapCatching { it.apply { validate() } }
  private fun readConfig(path: Path) = LocalFileSystem.read(path).flatMap<Config, String>(::deserialize)
  private fun dryRunRecreateDir(path: Path) = Result.success(Unit)
  private fun dryRunCopy(path: Path, path2: Path) = Result.success(Unit)
  private fun printMissingFiles(missingFiles: List<Path>) {
    println("The following files were found under the profile root folder and not marked up for backup or ignored. This could mean they are new files recently added. Please review and adjust your config:")
    missingFiles.forEach { println("  - $it") }
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
  }

  private fun printVersion() {
    println("dotsave v 1.0.0")
  }
}
