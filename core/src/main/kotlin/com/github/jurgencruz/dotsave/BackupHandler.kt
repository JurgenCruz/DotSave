package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.Config
import com.github.jurgencruz.dotsave.config.Profile
import com.github.jurgencruz.dotsave.logging.LogLevel
import com.github.jurgencruz.dotsave.utils.flatMap
import com.github.jurgencruz.dotsave.utils.mergeFailures
import java.nio.file.Path

/**
 * Handler for the backup process.
 */
@Suppress("HardCodedStringLiteral")
object BackupHandler {
  /**
   * Backup files based on the configuration on the specified path.
   * @param config The backup configuration.
   * @param backupPath The path of the directory to back up to.
   * @param profile The profile to execute.
   * @param log The logger function.
   * @param recreateDir function that deletes and creates a directory again.
   * @param copy function to copy a file from path a to path b.
   * @return A result object with the list of missing files if successful or exception if failure.
   */
  fun backup(
    config: Config,
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    recreateDir: (Path) -> Result<Unit>,
    copy: (Path, Path) -> Result<Unit>,
    walk: (Path) -> Sequence<Path>
  ) = backupWithLists(config, profile, backupPath, log, recreateDir, copy).map { (roots, copiedPaths, ignoredPaths) ->
    calculateMissingFiles(roots, copiedPaths, ignoredPaths, walk)
  }

  private fun backupWithLists(
    config: Config,
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    recreateDir: (Path) -> Result<Unit>,
    copy: (Path, Path) -> Result<Unit>
  ): Result<Triple<MutableList<Path>, MutableSet<Path>, MutableSet<Path>>> = profile.mergeInheritedProfiles(config).flatMap { p ->
    runIncludedProfiles(p, config, backupPath, log, recreateDir, copy).map { p to it }
  }.flatMap { (p, lists) ->
    runCatching { backupPath.resolve(p.name) to (p to lists) }
  }.onSuccess { (path) ->
    log(LogLevel.INFO, "Recreating directory: $path")
  }.flatMap { (path, p) ->
    recreateDir(path).map { p }
  }.onSuccess { (p, _) ->
    log(LogLevel.INFO, "Backing up profile: ${p.name}")
  }.flatMap { (profile, lists) ->
    backupFiles(profile, backupPath, log, copy).map { new ->
      calculateRootsAndFiles(
        lists.first,
        lists.second,
        lists.third,
        listOf(profile.rootPath),
        new,
        profile.ignorePath.map { profile.rootPath.resolve(it) },
        log
      )
      lists
    }
  }

  private fun calculateMissingFiles(
    roots: MutableList<Path>,
    copiedPaths: MutableSet<Path>,
    ignoredPaths: MutableSet<Path>,
    walk: (Path) -> Sequence<Path>
  ) = roots.flatMap {
    walk(it).toList()
  }.filter {
    notIncludedOrIgnored(copiedPaths, ignoredPaths, it)
  }

  private fun notIncludedOrIgnored(copiedPaths: MutableSet<Path>, ignoredPaths: MutableSet<Path>, file: Path) =
    !(ignoredPaths.any { file.startsWith(it) } || copiedPaths.any { file.startsWith(it) })

  private fun backupFiles(
    profile: Profile,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    copy: (Path, Path) -> Result<Unit>
  ) = backupPath.resolve(profile.name).let { bPath ->
    profile.includePath.asSequence().map { inc ->
      val srcPath = profile.rootPath.resolve(inc)
      val desPath = bPath.resolve(inc)
      log(LogLevel.INFO, "Copying '$srcPath' to '$desPath'")
      copy(srcPath, desPath).map { srcPath }
    }.mergeFailures()
  }

  private fun runIncludedProfiles(
    profile: Profile,
    config: Config,
    backupPath: Path,
    log: (LogLevel, String) -> Unit,
    recreateDir: (Path) -> Result<Unit>,
    copy: (Path, Path) -> Result<Unit>
  ): Result<Triple<MutableList<Path>, MutableSet<Path>, MutableSet<Path>>> = profile.includeProfiles.asSequence().map { name ->
    backupWithLists(config, config.profiles.first { (n) -> n == name }, backupPath, log, recreateDir, copy)
  }.mergeFailures().map { list ->
    val roots = mutableListOf<Path>()
    val copiedPaths = mutableSetOf<Path>()
    val ignoredPaths = mutableSetOf<Path>()
    list.forEach { (r, c, i) ->
      calculateRootsAndFiles(roots, copiedPaths, ignoredPaths, r, c.toList(), i.toList(), log)
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
    log: (LogLevel, String) -> Unit
  ) {
    newRoots.forEach { newRoot ->
      if (!roots.any { newRoot.startsWith(it) }) {
        roots.add(newRoot)
      }
    }
    newCopiedPaths.forEach {
      if (!copiedPaths.add(it)) {
        log(LogLevel.ERROR, "File '$it' is duplicated in the backup.")
      }
    }
    ignoredPaths.addAll(newIgnoredPaths)
  }
}
