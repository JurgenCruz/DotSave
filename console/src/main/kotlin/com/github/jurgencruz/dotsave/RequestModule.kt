package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.ConfigParser
import com.github.jurgencruz.dotsave.config.EnvVarReplacer
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import com.github.jurgencruz.dotsave.logging.ConsoleLogger
import com.github.jurgencruz.dotsave.logging.Logger
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [FileSystemModule::class])
internal object RequestModule {
  @Provides
  @Singleton
  fun provideConfigParser(fileSystem: FileSystem, envVarReplacer: EnvVarReplacer): ConfigParser = ConfigParser(fileSystem, envVarReplacer)

  @Provides
  @Singleton
  fun provideBackupHandler(fileSystem: FileSystem, logger: Logger): BackupHandler = BackupHandler(fileSystem, logger)

  @Provides
  @Singleton
  fun provideRestoreHandler(fileSystem: FileSystem, logger: Logger): RestoreHandler = RestoreHandler(fileSystem, logger)

  @Provides
  @Singleton
  fun provideLogger(@Named("verbose") verbose: Boolean): Logger = ConsoleLogger(verbose)

  @Provides
  @Singleton
  fun provideEnvVarParser(): EnvVarReplacer = EnvVarReplacer()
}
