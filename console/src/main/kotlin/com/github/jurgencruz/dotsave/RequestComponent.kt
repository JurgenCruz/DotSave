package com.github.jurgencruz.dotsave

import com.github.jurgencruz.dotsave.config.ConfigParser
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

/**
 * Component Interface for Dependency Injection for the Request Scope. Dagger will implement it in auto-generated
 * code.
 * @property configParser The configuration file parser.
 * @property backupHandler The handler for backup process.
 * @property restoreHandler The handler for restore process.
 */
@Singleton
@Component(modules = [RequestModule::class])
interface RequestComponent {
  val configParser: ConfigParser
  val backupHandler: BackupHandler
  val restoreHandler: RestoreHandler

  /**
   * A Builder for the RequestComponent.
   */
  @Component.Builder
  interface Builder {
    /**
     * Sets the logging level for the logger.
     * @param verbose Whether the logger should be verbose.
     * @return This builder itself. Used for chained building.
     */
    @BindsInstance
    fun withLogging(@Named("verbose") verbose: Boolean): Builder

    /**
     * Build the Component.
     * @return The built component.
     */
    fun build(): RequestComponent
  }
}
