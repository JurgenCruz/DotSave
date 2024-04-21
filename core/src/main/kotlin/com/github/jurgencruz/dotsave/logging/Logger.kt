package com.github.jurgencruz.dotsave.logging

/**
 * Logger class.
 */
interface Logger {
  /**
   * Log a message.
   * @param msg The message to log.
   */
  fun log(msg: String)

  /**
   * Log an error.
   * @param msg The error message to log.
   */
  fun error(msg: String)
}
