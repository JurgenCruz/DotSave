package com.github.jurgencruz.dotsave.config

import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import com.github.jurgencruz.dotsave.utils.toSafePath
import kotlinx.serialization.json.Json.Default

/**
 * Parser for the configuration file.
 * @constructor Create a new config parser.
 * @param fileSystem The file system layer.
 */
class ConfigParser(fileSystem: FileSystem, envVarReplacer: EnvVarReplacer) {
  private val mEnvVarReplacer = envVarReplacer
  private val mFileSystem = fileSystem

  /**
   * Parse the config file. The directory where the specified config file is located will be used to do the backup or
   * restore.
   * @param path The path of the configuration file and the directory where to do the backup to or restore from.
   * @return A result object with the config object if successful or exception if error.
   */
  fun parse(path: String) = toSafePath(path).flatMap(mFileSystem::read).mapCatching<Config, String>(Default::decodeFromString).flatMap(::replaceEnvVars).flatMap(::validateConfig)
  private fun validateConfig(config: Config): Result<Config> = when {
    config.profiles.distinctBy(Profile::name).size != config.profiles.size -> Result.failure(IllegalStateException("Config cannot contain two profiles with the same name"))
    config.profiles.count { it.default } > 1                               -> Result.failure(IllegalArgumentException("Config cannot contain more than one default profile"))
    config.profiles.any { it.name.isBlank() }                              -> Result.failure(IllegalArgumentException("Profile names cannot be blank. If using Env Vars, make sure they have valid values"))
    else                                                                   -> Result.success(config)
  }

  private fun replaceEnvVars(config: Config) = config.profiles.asSequence().map(::replaceEnvVars).mergeFailures().map { config.copy(profiles = it) }
  private fun replaceEnvVars(profile: Profile) = mEnvVarReplacer.replace(profile.name).map { profile.copy(name = it.trim()) }.flatMap { p ->
    mEnvVarReplacer.replace(p.root).map { p.copy(root = it) }
  }.flatMap { p ->
    replaceEnvVars(p.includeProfiles).map { p.copy(includeProfiles = it) }
  }.flatMap { p ->
    replaceEnvVars(p.inheritProfiles).map { p.copy(inheritProfiles = it) }
  }.flatMap { p ->
    replaceEnvVars(p.include).map { p.copy(include = it) }
  }.flatMap { p ->
    replaceEnvVars(p.exclude).map { p.copy(exclude = it) }
  }

  private fun replaceEnvVars(list: List<String>) = list.asSequence().map(mEnvVarReplacer::replaceMandatory).mergeFailures()
}
