package com.github.jurgencruz.dotsave.config

import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures

/**
 * Helper to replace environment variables in a string.
 */
@Suppress("HardCodedStringLiteral")
object EnvVarReplacer {
  private val regex = Regex("\\$(?<name>\\{[A-Za-z_]\\w+}|[A-Za-z_]\\w+)") // NON-NLS
  internal var getEnv: (String) -> String = ::getEnvVar

  /**
   * Replaces all Environment Variables in the config including in name, root, includeProfiles, inheritProfiles, include and ignore.
   * @param config The config to replace Env Vars on.
   * @return A result object that has the new config file with replaced environments or an error if it failed.
   */
  fun replaceEnvVars(config: Config) = config.profiles.asSequence().map(::replaceEnvVars).mergeFailures().map { config.copy(profiles = it) }
  private fun replaceEnvVars(profile: Profile) = replace(profile.name).map { profile.copy(name = it) }.flatMap { p ->
    replace(p.root).map { p.copy(root = it) }
  }.flatMap { p ->
    replaceAllItemsOnList(p.includeProfiles).map { p.copy(includeProfiles = it) }
  }.flatMap { p ->
    replaceAllItemsOnList(p.inheritProfiles).map { p.copy(inheritProfiles = it) }
  }.flatMap { p ->
    replaceAllItemsOnList(p.include).map { p.copy(include = it) }
  }.flatMap { p ->
    replaceAllItemsOnList(p.ignore).map { p.copy(ignore = it) }
  }

  private fun replace(string: String) = runCatching {
    regex.replace(string) { m ->
      m.groups["name"]!!.value.replace("{", "").replace("}", "").let(getEnv)
    }
  }

  private fun replaceAllItemsOnList(list: List<String>) = list.asSequence().map(::replace).mergeFailures()
  private fun getEnvVar(varName: String) = System.getenv(varName) ?: ""
}
