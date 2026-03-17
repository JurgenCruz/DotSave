package com.github.jurgencruz.dotsave.logging

/**
 * Logger that writes to console.
 */
@Suppress("HardCodedStringLiteral")
object ConsoleLogger {
  private const val RED = "\u001b[31m"
  private const val RESET = "\u001b[0m"
  /**
   * Log a message always.
   * @param level The level of the message.
   * @param msg The message to log.
   */
  fun verboseLog(level: LogLevel, msg: String) {
    if (level == LogLevel.ERROR) {
      println("$RED$msg$RESET")
    } else {
      println(msg)
    }
  }
  /**
   * Log a message only if it is an error.
   * @param level The level of the message.
   * @param msg The message to log.
   */
  fun log(level: LogLevel, msg: String) {
    if (level == LogLevel.ERROR) {
      println("$RED$msg$RESET")
    }
  }

  fun printErrors(ex: Throwable) {
    println("${RED}Error: $ex$RESET")
    ex.suppressed.forEach(::printErrors)
  }
}
