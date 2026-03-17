package com.github.jurgencruz.dotsave.config

import kotlinx.serialization.Serializable

/**
 * The configuration for backup and restore.
 * @constructor Create a new configuration.
 * @param profiles The different profiles that can be backed up and restored.
 */
@Serializable
data class Config(val profiles: List<Profile>) {
  fun validate() {
    require(profiles.isNotEmpty()) { "No profiles found in configuration." }
    profiles.forEach { it.validate() }
    require(profiles.distinctBy(Profile::name).size == profiles.size) { "Config cannot contain two profiles with the same name" }
    require(profiles.count { it.default } <= 1) { "Config cannot contain more than one default profile" }
    require(profiles.all { profile ->
      profile.includeProfiles.all { child ->
        profiles.any { it.name == child && profile.name != child }
      } && profile.inheritProfiles.all { child ->
        profiles.any { it.name == child && profile.name != child }
      }
    }) { "Profile references must exist" }
  }
}
