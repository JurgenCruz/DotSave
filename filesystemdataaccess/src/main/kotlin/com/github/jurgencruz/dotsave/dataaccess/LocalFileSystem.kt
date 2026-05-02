package com.github.jurgencruz.dotsave.dataaccess

import com.github.jurgencruz.dotsave.FileMetaData
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.exists
import kotlin.io.path.getOwner
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.setOwner
import kotlin.io.path.setPosixFilePermissions
import kotlin.streams.asSequence

/**
 * File System implementation of the Data Access Layer.
 * @constructor Create a new File System object.
 */
object LocalFileSystem {
  private val service = FileSystems.getDefault().userPrincipalLookupService

  fun getFileSystem(dryRun: Boolean): FileSystem {
    return if (dryRun)
      FileSystem(
        ::exists,
        ::isDirectory,
        ::isFile,
        ::dryRunDeleteDir,
        ::dryRunCreateDirectory,
        ::dryRunCopyFile,
        ::dryRunChangeOwnerAndAttrs,
        ::getMetadata,
        ::read,
        ::dryRunWrite,
        ::walk
      )
    else
      FileSystem(
        ::exists,
        ::isDirectory,
        ::isFile,
        ::deleteDir,
        ::createDirectory,
        ::copyFile,
        ::changeOwnerAndAttrs,
        ::getMetadata,
        ::read,
        ::write,
        ::walk
      )
  }

  private fun exists(path: Path) = path.exists()
  private fun isDirectory(path: Path) = path.isDirectory()
  private fun isFile(path: Path) = path.isRegularFile()
  private fun deleteDir(path: Path) = path.toFile().deleteRecursively()
  private fun dryRunDeleteDir(path: Path) = true
  private fun createDirectory(path: Path) {
    Files.createDirectory(path)
  }

  private fun dryRunCreateDirectory(path: Path) {}
  private fun copyFile(srcPath: Path, destPath: Path) = Files.copy(
    srcPath,
    destPath,
    LinkOption.NOFOLLOW_LINKS,
    StandardCopyOption.COPY_ATTRIBUTES,
    StandardCopyOption.REPLACE_EXISTING
  )

  private fun dryRunCopyFile(srcPath: Path, destPath: Path) {}
  private fun changeOwnerAndAttrs(path: Path, metadata: FileMetaData) {
    val owner = service.lookupPrincipalByName(metadata.owner)
    val group = service.lookupPrincipalByGroupName(metadata.owner)
    path.setOwner(owner)
    Files.getFileAttributeView(path, PosixFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS).setGroup(group)
    path.setPosixFilePermissions(PosixFilePermissions.fromString(metadata.permissions))
  }

  private fun dryRunChangeOwnerAndAttrs(path: Path, metadata: FileMetaData) {}
  private fun getMetadata(path: Path) = FileMetaData(
    path.getOwner(LinkOption.NOFOLLOW_LINKS)!!.name,
    path.getPosixFilePermissions(LinkOption.NOFOLLOW_LINKS).toString()
  )

  private fun read(path: Path) = runCatching { path.toFile().readText(Charsets.UTF_8) }
  private fun write(path: Path, data: String) = runCatching {
    path.toFile().writeText(data, Charsets.UTF_8)
  }

  private fun dryRunWrite(path: Path, data: String) = Result.success(Unit)
  private fun walk(path: Path, maxDepth: Int): Sequence<Path> = Files.walk(path, maxDepth).asSequence()
}
