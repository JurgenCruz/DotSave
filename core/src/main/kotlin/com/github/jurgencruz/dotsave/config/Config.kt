package com.github.jurgencruz.dotsave.config

import kotlinx.serialization.Serializable

/**
 * The configuration for backup and restore.
 * @constructor Create a new configuration.
 * @param profiles The different profiles that can be backed up and restored.
 */
@Serializable
data class Config(val profiles: List<Profile>)
