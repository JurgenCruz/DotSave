package com.github.jurgencruz.dotsave.logging

/**
 * Levels of logging available.
 */
enum class LogLevel {
  /**
   * Information level to log general messages.
   */
  INFO,

  /**
   * Warn level to log when something is not right but can continue.
   */
  WARN,

  /**
   * Error level to log when something goes wrong.
   */
  ERROR
}
