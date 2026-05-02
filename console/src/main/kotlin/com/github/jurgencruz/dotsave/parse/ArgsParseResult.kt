package com.github.jurgencruz.dotsave.parse

/**
 * The argument parsing result.
 * @constructor Create a new parsing result.
 * @param action The action the program should execute.
 * @param path The path where it should back up to or restore from.
 * @param verbose Whether the program should be verbose during execution.
 * @param profile The name of the profile in the config file to execute.
 * @param dryRun Enable simulation mode without actually doing anything on the filesystem.
 */
data class ArgsParseResult(
  val action: Action,
  val path: String,
  val verbose: Boolean,
  val profile: String?,
  val dryRun: Boolean,
  val owner: String?
)
