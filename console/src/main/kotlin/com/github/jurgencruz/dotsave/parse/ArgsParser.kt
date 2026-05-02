package com.github.jurgencruz.dotsave.parse

/**
 * The parser for the console arguments.
 */
@Suppress("HardCodedStringLiteral")
object ArgsParser {
  private const val HELP_SHORT = "-h"
  private const val HELP_LONG = "--help"
  private const val VERSION_SHORT = "-V"
  private const val VERSION_LONG = "--version"
  private const val VERBOSE_SHORT = "-v"
  private const val VERBOSE_LONG = "--verbose"
  private const val DRY_RUN_SHORT = "-d"
  private const val DRY_RUN_LONG = "--dry-run"
  private const val BACKUP_SHORT = "-b"
  private const val BACKUP_LONG = "--back-up"
  private const val RESTORE_SHORT = "-r"
  private const val RESTORE_LONG = "--restore"
  private const val PROFILE_SHORT = "-p"
  private const val PROFILE_LONG = "--profile"
  private const val OWNER_SHORT = "-o"
  private const val OWNER_LONG = "--owner"

  /**
   * Parse the console arguments.
   * @param args The console arguments to parse.
   * @return The result of the parsing.
   */
  fun parse(args: Array<String>): Result<ArgsParseResult> {
    var verbose = false
    var dryRun = false
    var action = Action.USAGE
    var path = ""
    var profile: String? = null
    var owner: String? = null
    var i = 0
    while (i < args.size) {
      val arg = args[i]
      ++i

      when {
        shouldShowUsage(arg)   -> {
          if (action != Action.USAGE) {
            return Result.failure(Exception("You can only specify one action"))
          }
          return Result.success(ArgsParseResult(Action.USAGE, "", false, null, false, null))
        }

        shouldShowVersion(arg) -> {
          if (action != Action.USAGE) {
            return Result.failure(Exception("You can only specify one action"))
          }
          return Result.success(ArgsParseResult(Action.VERSION, "", false, null, false, null))
        }

        isVerbose(arg)         -> {
          verbose = true
        }

        isDryRun(arg)          -> {
          dryRun = true
        }

        isSave(arg)            -> {
          if (action != Action.USAGE) {
            return Result.failure(Exception("You can only specify one action"))
          }
          action = Action.BACKUP
          if (i >= args.size) {
            return Result.failure(Exception("No path specified for saving"))
          }
          path = args[i]
          ++i
        }

        isApply(arg)           -> {
          if (action != Action.USAGE) {
            return Result.failure(Exception("You can only specify one action"))
          }
          action = Action.RESTORE
          if (i >= args.size) {
            return Result.failure(Exception("No path specified for restoring"))
          }
          path = args[i]
          ++i
        }

        isProfileName(arg)     -> {
          if (profile != null) {
            return Result.failure(Exception("You can only specify one profile"))
          }
          if (i >= args.size) {
            return Result.failure(Exception("No profile name specified"))
          }
          profile = args[i]
          ++i
        }

        isOwner(arg)           -> {
          if (owner != null) {
            return Result.failure(Exception("You can only specify one owner"))
          }
          if (i >= args.size) {
            return Result.failure(Exception("No owner specified"))
          }
          owner = args[i]
          ++i
        }

        else                   -> return Result.failure(Exception("Unrecognized argument: $arg"))
      }
    }
    return Result.success(ArgsParseResult(action, path, verbose, profile, dryRun, owner))
  }

  private fun shouldShowUsage(arg: String): Boolean = arg == HELP_SHORT || arg == HELP_LONG
  private fun shouldShowVersion(arg: String): Boolean = arg == VERSION_SHORT || arg == VERSION_LONG
  private fun isVerbose(arg: String): Boolean = arg == VERBOSE_SHORT || arg == VERBOSE_LONG
  private fun isDryRun(arg: String): Boolean = arg == DRY_RUN_SHORT || arg == DRY_RUN_LONG
  private fun isSave(arg: String): Boolean = arg == BACKUP_SHORT || arg == BACKUP_LONG
  private fun isApply(arg: String): Boolean = arg == RESTORE_SHORT || arg == RESTORE_LONG
  private fun isProfileName(arg: String): Boolean = arg == PROFILE_SHORT || arg == PROFILE_LONG
  private fun isOwner(arg: String): Boolean = arg == OWNER_SHORT || arg == OWNER_LONG
}
