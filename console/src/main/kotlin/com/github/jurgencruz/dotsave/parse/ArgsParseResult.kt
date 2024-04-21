package com.github.jurgencruz.dotsave.parse

/**
 * The argument parsing result.
 * @constructor Create a new parsing result.
 * @param action The action the program should execute.
 * @param path The path where it should back up to or restore from.
 * @param verbose Whether the program should be verbose during execution.
 */
data class ArgsParseResult(val action: Action, val path: String, val verbose: Boolean)
