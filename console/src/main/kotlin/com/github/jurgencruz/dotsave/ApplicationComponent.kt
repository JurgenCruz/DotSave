package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.parse.ArgsParser
import dagger.Component
import javax.inject.Singleton

/**
 * Component Interface for Dependency Injection for the Application Scope. Dagger will implement it in auto-generated
 * code.
 * @property argsParser The console arguments parser.
 */
@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {
  val argsParser: ArgsParser
}
