package com.github.jurgencruz.dotsave.config

import kotlinx.serialization.Serializable

/**
 * The configuration for backup and restore.
 * @constructor Create a new configuration.
 * @param profiles The different profiles that can be backed up and restored.
 */
@Suppress("HardCodedStringLiteral")
@Serializable
data class Config(val profiles: List<Profile>) {
  /**
   * Validates the configuration for backup and restore.
   *
   * Ensures that:
   * - The profiles list is not empty.
   * - Each profile in the list is valid.
   * - All profiles have unique names.
   * - There is at most one default profile.
   * - All included and inherited profiles exist within the configuration.
   */
  fun validate() {
    require(profiles.isNotEmpty()) { "No profiles found in configuration." }
    profiles.forEach { it.validate() }
    require(profiles.distinctBy(Profile::name).size == profiles.size) { "Config cannot contain two profiles with the same name." }
    require(profiles.count { it.default } <= 1) { "Config cannot contain more than one default profile." }
    profiles.forEach { profile ->
      profile.includeProfiles.forEach { reference ->
        require(profiles.any { it.name == reference && profile.name != reference }) { "Included profile: $reference does not exist in the configuration. Profile: ${profile.name}." }
      }
      profile.inheritProfiles.forEach { reference ->
        require(profiles.any { it.name == reference && profile.name != reference }) { "Inherited profile: $reference does not exist in the configuration. Profile: ${profile.name}." }
      }
    }
  }

  /**
   * Selects a profile from the configuration based on the provided profile name.
   *
   * If no profile name is specified, the method attempts to select the default profile.
   * If there is no default profile and no profile name is provided, an error is returned.
   * If the specified profile name does not exist in the configuration, an error is returned.
   *
   * @param profileName The name of the profile to select. If null or blank, the default profile will be selected.
   * @return A Result containing the selected Profile if successful, or a failure otherwise.
   */
  fun selectProfile(profileName: String?): Result<Profile> {
    val profile = if (profileName.isNullOrBlank()) {
      profiles.firstOrNull { it.default } ?: return Result.failure(IllegalStateException("No default profile in config file and no profile name specified."))
    } else {
      profiles.firstOrNull { it.name == profileName } ?: return Result.failure(IllegalStateException("No profile with name: $profileName exists."))
    }
    return Result.success(profile)
  }
}
