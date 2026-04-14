package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import java.nio.file.Path

/**
 * Contains context for the backup and restore operations.
 * @param path The path of the folder to backup to or restore from.
 * @param config The configuration for the process.
 * @param profile The selected profile from the configuration.
 */
data class Context(val path: Path, val config: Config, val profile: Profile)
