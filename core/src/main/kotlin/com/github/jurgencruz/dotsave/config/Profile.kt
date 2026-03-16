package com.github.jurgencruz.dotsave.config

import kotlinx.serialization.Serializable

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
data class Profile(var name: String, var default: Boolean, var root: String, var includeProfiles: List<String>, var inheritProfiles: List<String>, var include: List<String>, var exclude: List<String>)
