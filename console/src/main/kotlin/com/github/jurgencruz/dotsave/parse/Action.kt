package com.github.jurgencruz.dotsave.parse

/**
 * The different actions that the console can handle.
 */
enum class Action {
  /**
   * Print the usage help.
   */
  USAGE,

  /**
   * Print the version.
   */
  VERSION,

  /**
   * Backup the dot files.
   */
  BACKUP,

  /**
   * Restore the dot files.
   */
  RESTORE
}
