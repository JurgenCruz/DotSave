package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.ConfigValidator
import com.github.jurgencruz.dotsave.config.EnvVarReplacer
import com.github.jurgencruz.dotsave.dataaccess.LocalFileSystem
import com.github.jurgencruz.dotsave.logging.ConsoleLogger
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.parse.Action
import com.github.jurgencruz.dotsave.parse.ArgsParser
import com.github.jurgencruz.dotsave.utils.deserialize
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.toSafePath
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
    val printErrors = ConsoleLogger::printErrors
    ArgsParser.parse(args).onFailure {
      printErrors(it)
      printUsage()
      exitProcess(1)
    }.onSuccess { (action, path, verbose, profile) ->
      val log = if (verbose) ConsoleLogger::verboseLog else ConsoleLogger::log
      when (action) {
        Action.USAGE   -> printUsage()
        Action.VERSION -> printVersion()
        Action.BACKUP  -> backup(path, verbose, profile, log, printErrors)
        Action.RESTORE -> restore(path, verbose, profile, log, printErrors)
      }
    }
  }

  private fun backup(path: String, verbose: Boolean, profile: String?, log: (LogLevel, String) -> Unit, printErrors: (ex: Throwable) -> Unit) {
    getConfig(path).flatMap { config ->
      BackupHandler.backup(config, path, profile, log, LocalFileSystem::recreateDir, LocalFileSystem::copy)
    }.onFailure {
      printErrors(it)
      exitProcess(2)
    }
  }

  private fun restore(path: String, verbose: Boolean, profile: String?, log: (LogLevel, String) -> Unit, printErrors: (ex: Throwable) -> Unit) {
    getConfig(path).flatMap { config ->
      RestoreHandler.restore(config, path, profile, log, LocalFileSystem::copy)
    }.onFailure {
      printErrors(it)
      exitProcess(3)
    }
  }

  private fun getConfig(path: String): Result<Config> = readConfig(path).flatMap(EnvVarReplacer::replaceEnvVars).flatMap(ConfigValidator::validate)
  private fun readConfig(path: String): Result<Config> = toSafePath(path).flatMap(LocalFileSystem::read).mapCatching(::deserialize)
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
    println("    -p, --profile            Select which profile to execute.")
  }

  private fun printVersion() {
    println("dotsave v 1.0.0")
  }
}
