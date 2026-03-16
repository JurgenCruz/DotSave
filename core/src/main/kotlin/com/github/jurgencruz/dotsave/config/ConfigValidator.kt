package com.github.jurgencruz.dotsave.config

/**
 * Validator for the configuration file.
 */
object ConfigValidator {
  /**
   * Validate the config file.
   * @param config The config to validate.
   * @return A result object with the config object if valid or exception if not.
   */
  fun validate(config: Config): Result<Config> = when {
    areProfilesNotUnique(config) -> Result.failure(IllegalStateException("Config cannot contain two profiles with the same name"))
    isMoreThanOneDefault(config) -> Result.failure(IllegalStateException("Config cannot contain more than one default profile"))
    areNamesInvalid(config)      -> Result.failure(IllegalStateException("Profile names cannot be blank. If using Env Vars, make sure they have valid values"))
    hasMissingChildren(config)   -> Result.failure(IllegalStateException("Profile references must exist"))
    else                         -> Result.success(config)
  }

  private fun areNamesInvalid(config: Config): Boolean = config.profiles.any { it.name.isBlank() }
  private fun areProfilesNotUnique(config: Config): Boolean = config.profiles.distinctBy(Profile::name).size != config.profiles.size
  private fun isMoreThanOneDefault(config: Config): Boolean = config.profiles.count { it.default } > 1
  private fun hasMissingChildren(config: Config): Boolean = config.profiles.any { profile ->
    profile.includeProfiles.any { child ->
      config.profiles.none { it.name == child && profile.name != child }
    } || profile.inheritProfiles.any { child ->
      config.profiles.none { it.name == child && profile.name != child }
    }
  }
}
