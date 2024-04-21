package com.github.jurgencruz.dotsave.parse

/**
 * The parser for the console arguments.
 * @constructor Create a new Argument Parser.
 */
@Suppress("HardCodedStringLiteral")
class ArgsParser {
  companion object {
    private const val HELP_SHORT = "-h"
    private const val HELP_LONG = "--help"
    private const val VERSION_SHORT = "-V"
    private const val VERSION_LONG = "--version"
    private const val VERBOSE_SHORT = "-v"
    private const val VERBOSE_LONG = "--verbose"
    private const val BACKUP_SHORT = "-b"
    private const val BACKUP_LONG = "--back-up"
    private const val RESTORE_SHORT = "-r"
    private const val RESTORE_LONG = "--restore"
  }

  /**
   * Parse the console arguments.
   * @param args The console arguments to parse.
   * @return The result of the parsing.
   */
  fun parse(args: Array<String>): Result<ArgsParseResult> {
    var verbose = false
    var action = Action.USAGE
    var path = ""
    var i = 0
    while (i < args.size) {
      val arg = args[i]
      ++i
      if (shouldShowUsage(arg)) {
        action = Action.USAGE
        break
      }
      if (shouldShowVersion(arg)) {
        action = Action.VERSION
        break
      }
      if (isVerbose(arg)) {
        verbose = true
        continue
      }
      if (action != Action.USAGE) {
        return Result.failure(Exception("You can only specify one action"))
      }
      when {
        isSave(arg)  -> {
          action = Action.BACKUP
          if (i >= args.size) {
            return Result.failure(Exception("No path specified for saving"))
          }
          path = args[i]
          ++i
        }

        isApply(arg) -> {
          action = Action.RESTORE
          if (i >= args.size) {
            return Result.failure(Exception("No path specified for applying"))
          }
          path = args[i]
          ++i
        }

        else         -> return Result.failure(Exception("Unrecognized argument: $arg"))
      }
    }
    return Result.success(ArgsParseResult(action, path, verbose))
  }

  private fun shouldShowUsage(arg: String): Boolean = arg == HELP_SHORT || arg == HELP_LONG
  private fun shouldShowVersion(arg: String): Boolean = arg == VERSION_SHORT || arg == VERSION_LONG
  private fun isVerbose(arg: String): Boolean = arg == VERBOSE_SHORT || arg == VERBOSE_LONG
  private fun isSave(arg: String): Boolean = arg == BACKUP_SHORT || arg == BACKUP_LONG
  private fun isApply(arg: String): Boolean = arg == RESTORE_SHORT || arg == RESTORE_LONG
}
