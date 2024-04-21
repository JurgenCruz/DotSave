package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.parse.ArgsParser
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal object ApplicationModule {
  @Provides
  @Singleton
  fun provideArgsParser(): ArgsParser = ArgsParser()
}
