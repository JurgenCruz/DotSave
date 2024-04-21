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
  fun parse(path: String): Result<Config> {
    return toSafePath(path).flatMap(mFileSystem::read).mapCatching<Config, String>(Default::decodeFromString).flatMap(::replaceEnvVar).flatMap { config ->
      if (config.profiles.distinctBy(Profile::name).size != config.profiles.size) {
        Result.failure(IllegalStateException("Config cannot contain two profiles with the same name"))
      } else {
        Result.success(config)
      }
    }
  }

  private fun replaceEnvVar(config: Config): Result<Config> {
    return config.profiles.asSequence().map { profile ->
      mEnvVarReplacer.replace(profile.name).onSuccess {
        profile.name = it
      }.flatMap {
        mEnvVarReplacer.replace(profile.root)
      }.onSuccess {
        profile.root = it
      }.flatMap {
        val newInclude = mutableListOf<String>()
        (profile.include.asSequence().map(mEnvVarReplacer::replace).onEach {
          it.onSuccess(newInclude::add)
        }.firstOrNull(Result<String>::isFailure)?.map { } ?: Result.success(Unit)).onSuccess { profile.include = newInclude }
      }
    }.mergeFailures().map { config }
  }
}
