package com.github.jurgencruz.dotsave.logging

/**
 * Logger that writes to console.
 * @constructor Create a new logger.
 * @param verbose Whether to be a verbose logger.
 */
class ConsoleLogger(verbose: Boolean) : Logger {
  private val mVerbose = verbose
  override fun log(msg: String) {
    if (mVerbose) {
      println(msg)
    }
  }

  override fun error(msg: String) {
    println(msg)
  }
}
