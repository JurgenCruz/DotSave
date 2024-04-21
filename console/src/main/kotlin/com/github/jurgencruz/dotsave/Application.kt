package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.parse.Action
import com.github.jurgencruz.dotsave.utils.flatMap
import kotlin.system.exitProcess

/**
 * Entry class for Application.
 */
@Suppress("HardCodedStringLiteral")
object Application {
  private const val RED = "\u001b[31m"
  private const val RESET = "\u001b[0m"

  /**
   * Entry point for Application.
   */
  @JvmStatic
  fun main(args: Array<String>) {
    val appComponent = DaggerApplicationComponent.builder().build()
    val argsParser = appComponent.argsParser
    argsParser.parse(args).onFailure {
      println(it.message)
      printUsage()
      exitProcess(1)
    }.onSuccess { (action, path, verbose) ->
      when (action) {
        Action.USAGE   -> printUsage()
        Action.VERSION -> printVersion()
        Action.BACKUP  -> backup(path, verbose)
        Action.RESTORE -> restore(path, verbose)
      }
    }
  }

  private fun backup(path: String, verbose: Boolean) {
    val requestComponent = DaggerRequestComponent.builder().withLogging(verbose).build()
    val configParser = requestComponent.configParser
    configParser.parse(path).flatMap { config ->
      val backupHandler = requestComponent.backupHandler
      backupHandler.backup(config, path)
    }.onFailure {
      printErrors(it)
      exitProcess(2)
    }
  }

  private fun restore(path: String, verbose: Boolean) {
    val requestComponent = DaggerRequestComponent.builder().withLogging(verbose).build()
    val configParser = requestComponent.configParser
    configParser.parse(path).flatMap { config ->
      val restoreHandler = requestComponent.restoreHandler
      restoreHandler.restore(config, path)
    }.onFailure {
      printErrors(it)
      exitProcess(3)
    }
  }

  private fun printErrors(ex: Throwable) {
    println("${RED}Error: $ex$RESET")
    ex.suppressed.forEach(::printErrors)
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
  }

  private fun printVersion() {
    println("dotsave v 1.0.0")
  }
}
