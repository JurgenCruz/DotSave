package com.github.jurgencruz.dotsave.config

import com.github.jurgencruz.dotsave.utils.flatMap
import kotlinx.serialization.Serializable
import kotlin.io.path.Path

/**
 * A Profile that specifies how to back up and restore something, for example a program config files.
 * @constructor Create a new Profile.
 * @param name The name of the profile.
 * @param default Whether this profile is the default profile in the configuration.
 * @param root The root path of the files.
 * @param includeProfiles List of profiles to execute before executing this one.
 * @param inheritProfiles List of profiles to merge into this profile before executing.
 * @param include List of files and directories to include in the backup or restore.
 * @param exclude List of files and directories to exclude in the backup. Used mainly to disable warnings about files not being backed up.
 */
@Serializable
data class Profile(val name: String, val default: Boolean, val root: String, val includeProfiles: List<String>, val inheritProfiles: List<String>, val include: List<String>, val exclude: List<String>) {
  companion object {
    fun mergeProfile(config: Config, profile: Profile): Result<Profile> {
      return profile.inheritProfiles.fold(Result.success(profile)) { profile, toInheritName ->
        profile.flatMap { profile ->
          mergeProfile(config, config.profiles.first { (n) -> n == toInheritName }).map { it to profile }
        }.map { (toInherit, profile) ->
          val prefix = Path(profile.root).relativize(Path(toInherit.root))
          val newInclude = profile.include.toMutableSet()
          newInclude.addAll(toInherit.include.map { prefix.resolve(it).toString() })
          val newExclude = profile.exclude.toMutableSet()
          newExclude.addAll(toInherit.exclude.map { prefix.resolve(it).toString() })
          val newIncludeProfiles = profile.includeProfiles.toMutableSet()
          newIncludeProfiles.addAll(toInherit.includeProfiles)
          profile.copy(include = newInclude.toList(), exclude = newExclude.toList(), includeProfiles = newIncludeProfiles.toList())
        }
      }
    }
  }
}
