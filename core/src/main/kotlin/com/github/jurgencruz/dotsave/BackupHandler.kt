package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import com.github.jurgencruz.dotsave.utils.serialize
import java.nio.file.Path

/**
 * Handler for the backup process.
 */
@Suppress("HardCodedStringLiteral")
object BackupHandler {
  private const val PERMISSIONS = "rw-rw-rw-"

  /**
   * Backup files based on the configuration on the specified path.
   * @param config The backup configuration.
   * @param profile The profile to execute.
   * @param backupPath The path of the directory to back up to.
   * @param log The logger function.
   * @param fileSystem The group of functions to interact with the File System.
   * @return A result object with the list of missing files if successful or exception if failure.
   */
  fun backup(
    config: Config,
    profile: Profile,
    backupPath: Path,
    owner: String,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ) = backupWithLists(
    config,
    profile,
    backupPath,
    FileMetaData(owner, PERMISSIONS),
    log,
    fileSystem
  ).map { (roots, copiedPaths, ignoredPaths) ->
    calculateMissingFiles(roots, copiedPaths, ignoredPaths, fileSystem)
  }

  private fun backupWithLists(
    config: Config,
    profile: Profile,
    backupPath: Path,
    metaData: FileMetaData,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ): Result<Triple<MutableList<Path>, MutableSet<Path>, MutableSet<Path>>> = profile.mergeInheritedProfiles(config).flatMap { p ->
    runIncludedProfiles(p, config, backupPath, metaData, log, fileSystem).map { p to it }
  }.flatMap { (p, lists) ->
    runCatching { backupPath.resolve(p.name) to (p to lists) }
  }.onSuccess { (path) ->
    log(LogLevel.INFO, "Recreating directory: $path")
  }.flatMap { (path, pair) ->
    recreateDir(path, fileSystem, metaData).map { pair }
  }.onSuccess { (p, _) ->
    log(LogLevel.INFO, "Backing up profile: ${p.name}")
  }.flatMap { (profile, lists) ->
    backupFiles(profile, backupPath, metaData, log, fileSystem).map { new ->
      calculateRootsAndFiles(
        lists.first,
        lists.second,
        lists.third,
        listOf(profile.rootPath),
        new,
        profile.ignorePath.map { profile.rootPath.resolve(it) },
        log,
        fileSystem
      )
      lists
    }
  }

  private fun runIncludedProfiles(
    profile: Profile,
    config: Config,
    backupPath: Path,
    metaData: FileMetaData,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ): Result<Triple<MutableList<Path>, MutableSet<Path>, MutableSet<Path>>> = profile.includeProfiles.asSequence().map { name ->
    backupWithLists(config, config.profiles.first { (n) -> n == name }, backupPath, metaData, log, fileSystem)
  }.mergeFailures().map { list ->
    val roots = mutableListOf<Path>()
    val copiedPaths = mutableSetOf<Path>()
    val ignoredPaths = mutableSetOf<Path>()
    list.forEach { (r, c, i) ->
      calculateRootsAndFiles(roots, copiedPaths, ignoredPaths, r, c.toList(), i.toList(), log, fileSystem)
    }
    Triple(roots, copiedPaths, ignoredPaths)
  }

  private fun calculateRootsAndFiles(
    roots: MutableList<Path>,
    copiedPaths: MutableSet<Path>,
    ignoredPaths: MutableSet<Path>,
    newRoots: List<Path>,
    newCopiedPaths: List<Path>,
    newIgnoredPaths: List<Path>,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ) {
    newRoots.forEach { newRoot ->
      if (!roots.any { newRoot.startsWith(it) }) {
        roots.removeIf { it.startsWith(newRoot) }
        roots.add(newRoot)
      }
    }
    newCopiedPaths.forEach {
      if (!copiedPaths.add(it) && !fileSystem.isDirectory(it)) {
        log(LogLevel.ERROR, "File '$it' is duplicated in the backup.")
      }
    }
    ignoredPaths.addAll(newIgnoredPaths)
  }

  private fun recreateDir(path: Path, fileSystem: FileSystem, metaData: FileMetaData): Result<Unit> {
    if (fileSystem.exists(path)) {
      if (!fileSystem.isDirectory(path)) {
        return Result.failure(IllegalStateException("Path '$path' is not a directory, cannot recreate."))
      }
      if (!fileSystem.deleteDir(path)) {
        return Result.failure(IllegalStateException("Could not delete directory."))
      }
    }
    return runCatching {
      fileSystem.createDirectory(path)
      fileSystem.changeOwnerAndAttrs(path, metaData)
    }
  }

  private fun backupFiles(
    profile: Profile,
    backupPath: Path,
    metaData: FileMetaData,
    log: (LogLevel, String) -> Unit,
    fileSystem: FileSystem
  ) = backupPath.resolve(profile.name).let { bPath ->
    val metaDatas = mutableMapOf<Path, FileMetaData>()
    profile.includePath.asSequence().map { inc ->
      copy(profile.rootPath, bPath, inc, fileSystem, metaData, log).onSuccess { map ->
        metaDatas.putAll(map)
      }.map {}
    }.mergeFailures().flatMap {
      serialize(metaDatas.mapKeys { (key) -> "$key" })
    }.flatMap { mds ->
      val mp = backupPath.resolve("${profile.name}.json")
      fileSystem.write(mp, mds).mapCatching {
        fileSystem.changeOwnerAndAttrs(mp, metaData)
      }.map { metaDatas.keys.toList() }
    }
  }

  private fun copy(
    rootPath: Path,
    backupPath: Path,
    relativePath: Path,
    fileSystem: FileSystem,
    metaData: FileMetaData,
    log: (LogLevel, String) -> Unit
  ): Result<Map<Path, FileMetaData>> {
    val srcPath = rootPath.resolve(relativePath)
    val destPath = backupPath.resolve(relativePath)
    log(LogLevel.INFO, "Copying '$srcPath' to '$destPath'")
    if (!fileSystem.exists(srcPath)) {
      return Result.failure(IllegalStateException("Path '$srcPath' does not exist, cannot copy to '$destPath'."))
    }
    return if (fileSystem.isFile(srcPath)) {
      runCatching {
        val map = createParentDirs(srcPath.parent, destPath.parent, fileSystem, metaData)
        fileSystem.copyFile(srcPath, destPath)
        fileSystem.changeOwnerAndAttrs(destPath, metaData)
        map[srcPath] = fileSystem.getMetadata(srcPath)
        map
      }
    } else {
      runCatching {
        val map = createParentDirs(srcPath.parent, destPath.parent, fileSystem, metaData)
        fileSystem.copyFile(srcPath, destPath)
        fileSystem.changeOwnerAndAttrs(destPath, metaData)
        fileSystem.walk(srcPath, 1) to map
      }.flatMap { (files, map) ->
        files.drop(1).map { walkFile ->
          copy(srcPath, destPath, walkFile.fileName, fileSystem, metaData, log)
        }.mergeFailures().map { maps ->
          buildMap {
            putAll(map)
            maps.forEach(::putAll)
            put(srcPath, fileSystem.getMetadata(srcPath))
          }
        }
      }
    }
  }

  private fun createParentDirs(
    srcPath: Path?,
    destPath: Path?,
    fileSystem: FileSystem,
    metaData: FileMetaData
  ): MutableMap<Path, FileMetaData> = if (destPath != null && srcPath != null && !fileSystem.exists(destPath)) {
    val map = createParentDirs(srcPath.parent, destPath.parent, fileSystem, metaData)
    fileSystem.copyFile(srcPath, destPath)
    fileSystem.changeOwnerAndAttrs(destPath, metaData)
    map[srcPath] = fileSystem.getMetadata(srcPath)
    map
  } else {
    mutableMapOf()
  }

  private fun calculateMissingFiles(
    roots: MutableList<Path>,
    copiedPaths: MutableSet<Path>,
    ignoredPaths: MutableSet<Path>,
    fileSystem: FileSystem
  ) = roots.flatMap {
    fileSystem.walk(it).drop(1).toList()
  }.filter {
    notIncludedOrIgnoredOrDirectories(copiedPaths, ignoredPaths, it, fileSystem)
  }

  private fun notIncludedOrIgnoredOrDirectories(
    copiedPaths: MutableSet<Path>,
    ignoredPaths: MutableSet<Path>,
    file: Path,
    fileSystem: FileSystem
  ) = !(fileSystem.isDirectory(file) || ignoredPaths.any { file.startsWith(it) } || copiedPaths.any { file == it })
}
