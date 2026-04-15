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
 * @param ignore List of files and directories to ignore in the backup. Used mainly to disable warnings about files not being backed up.
 */
@Serializable
data class Profile(
  val name: String,
  val root: String,
  val include: List<String> = emptyList(),
  val ignore: List<String> = emptyList(),
  val includeProfiles: List<String> = emptyList(),
  val inheritProfiles: List<String> = emptyList(),
  val default: Boolean = false
) {
  companion object {
    fun mergeProfile(config: Config, profile: Profile): Result<Profile> {
      return profile.inheritProfiles.fold(Result.success(profile)) { profile, toInheritName ->
        profile.flatMap { profile ->
          mergeProfile(config, config.profiles.first { (n) -> n == toInheritName }).map { it to profile }
        }.map { (toInherit, profile) ->
          val prefix = Path(profile.root).relativize(Path(toInherit.root))
          val newInclude = profile.include.toMutableSet()
          newInclude.addAll(toInherit.include.map { prefix.resolve(it).toString() })
          val newIgnore = profile.ignore.toMutableSet()
          newIgnore.addAll(toInherit.ignore.map { prefix.resolve(it).toString() })
          val newIncludeProfiles = profile.includeProfiles.toMutableSet()
          newIncludeProfiles.addAll(toInherit.includeProfiles)
          profile.copy(include = newInclude.toList(), ignore = newIgnore.toList(), includeProfiles = newIncludeProfiles.toList())
        }
      }
    }
  }

  fun validate() {
    require(name.isNotBlank()) { "Profile name cannot be blank. If using Env Vars, make sure they have valid values." }
    require(root.isNotBlank()) { "Root cannot be blank. Profile: $name. If using Env Vars, make sure they have valid values." }
    require(include.isNotEmpty() || ignore.isNotEmpty() || includeProfiles.isNotEmpty() || inheritProfiles.isNotEmpty()) { "Either include, ignore, includeProfiles or inheritProfiles must include at least 1 item. Profile: $name." }
    require(includeProfiles.all { it.isNotBlank() }) { "IncludeProfile items cannot be blank. Profile: $name. If using Env Vars, make sure they have valid values." }
    require(inheritProfiles.all { it.isNotBlank() }) { "InheritProfile items cannot be blank. Profile: $name. If using Env Vars, make sure they have valid values." }
    require(include.all { it.isNotBlank() }) { "Include items cannot be blank. Profile: $name. If using Env Vars, make sure they have valid values." }
    require(ignore.all { it.isNotBlank() }) { "Ignore items cannot be blank. Profile: $name. If using Env Vars, make sure they have valid values." }
  }
}
