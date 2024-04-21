package com.github.jurgencruz.dotsave.config

import kotlinx.serialization.Serializable

/**
 * A Profile that specifies how to back up and restore something, for example a program config files.
 * @constructor Create a new Profile.
 * @param name The name of the profile.
 * @param root The root path of the files.
 * @param include List of files and directories to include in the backup or restore.
 */
@Serializable
data class Profile(var name: String, var root: String, var include: List<String>)
