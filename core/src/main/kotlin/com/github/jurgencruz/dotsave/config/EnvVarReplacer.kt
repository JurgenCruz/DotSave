package com.github.jurgencruz.dotsave.config

import com.github.jurgencruz.dotsave.utils.flatMap

/**
 * Helper to replace environment variables in a string.
 * @constructor Create a new helper.
 */
@Suppress("HardCodedStringLiteral")
open class EnvVarReplacer {
  companion object {
    private val regex = Regex("\\$(?<name>\\{[A-Za-z_]\\w+}|[A-Za-z_]\\w+)") //NON-NLS
  }

  /**
   * Replace any occurrence of a system variable with its value. A system variable start is denoted with a '$', the name
   * of a system variable cannot start with a digit, and the name can only contain alphanumeric character and
   * underscore.
   * @param string The string to do the replacements to.
   * @return A result object with the replaced string if successful or exception if error.
   */
  open fun replace(string: String): Result<String> {
    return runCatching {
      regex.replace(string) { m ->
        m.groups["name"]!!.value.replace("{", "").replace("}", "").let(::getEnv)
      }
    }
  }

  open fun replaceMandatory(string: String) = replace(string).map(String::trim).flatMap {
    if (it.isBlank()) {
      Result.failure(IllegalStateException("Value cannot be blank"))
    } else {
      Result.success(it)
    }
  }

  internal open fun getEnv(varName: String) = System.getenv(varName) ?: ""
}
