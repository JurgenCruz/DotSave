package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.dataaccess.LocalFileSystem
import com.github.jurgencruz.dotsave.dataaccess.FileSystem
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal object FileSystemModule {
  @Provides
  @Singleton
  fun provideFileSystem(): FileSystem = LocalFileSystem()
}
