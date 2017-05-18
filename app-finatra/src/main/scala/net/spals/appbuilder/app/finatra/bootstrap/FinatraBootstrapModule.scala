package net.spals.appbuilder.app.finatra.bootstrap

import com.google.common.base.Preconditions.checkState
import com.google.inject.{AbstractModule, Module}
import com.netflix.governator.LifecycleModule
import com.netflix.governator.guice.{BootstrapBinder, BootstrapModule, LifecycleInjector, ModuleListBuilder}
import com.netflix.governator.lifecycle.LifecycleConfigurationProviders
import com.twitter.inject.{Logging, TwitterModule}
import com.typesafe.config.{Config, ConfigFactory}
import net.spals.appbuilder.annotations.service.AutoBindModule
import net.spals.appbuilder.app.core.bootstrap.{AutoBindModulesBootstrapModule, BootstrapModuleWrapper}
import net.spals.appbuilder.config.provider.TypesafeConfigurationProvider
import net.spals.appbuilder.config.service.ServiceScan

import scala.collection.JavaConverters._

/**
  * Service to process [[AutoBindModule]] annotated
  * modules.
  *
  * Note that this is a replacement for [[AutoBindModulesBootstrapModule]]
  * because Finatra cannot handle Governator [[BootstrapModule]]s.
  *
  * @author tkral
  */
private[finatra] case class FinatraBootstrapModule(
    serviceConfig: Config = ConfigFactory.empty(),
    serviceScan: ServiceScan = ServiceScan.empty(),
    staticBootstrapModules: Seq[Module] = List()
  )
  extends TwitterModule
  with Logging {

  // A guice injector created for bootstraping via Governator
  private lazy val bootstrapInjector = {
    // Combine the configuration provider with all static bootstrap modules
    // given to us. Note that the configuration provider is bound first.
    val allStaticBootstrapModules = List(governatorPreBootstrapModule) ++
      staticBootstrapModules.map(new BootstrapModuleWrapper(_))
    // Use Governator to create an injector based on the static bootstrap modules
    LifecycleInjector.bootstrap(classOf[AnyRef] /*dummy*/, allStaticBootstrapModules: _*)
  }

  private lazy val configProvider = new TypesafeConfigurationProvider(serviceConfig)

  // BootstrapModule required to create the boostrapInjector through Governator.
  // This will be run first, before any other modules, while creating the bootstrap
  // injector.
  private lazy val governatorPreBootstrapModule = new BootstrapModule {
    override def configure(bootstrapBinder: BootstrapBinder): Unit = {
      bootstrapBinder.bindConfigurationProvider().toInstance(configProvider)
    }
  }

  // Module to be run before any other installed modules in Finatra.
  private lazy val finatraPreBootstrapModule = new AbstractModule {
    override def configure(): Unit = {
      // Activate Governator's lifecycle feature
      install(new LifecycleModule())
      // Bind a LifecycleCOnfigurationProviders instance so as to activate Governator's
      // @Configuration feature. Note that this is slightly different then what we do in
      // the Governator pre bootstrap module above since there the Governator internals
      // are taking care of these mechanics for us.
      bind(classOf[LifecycleConfigurationProviders]).toInstance(new LifecycleConfigurationProviders(configProvider))
    }
  }

  // Modules installed into Finatra
  override lazy val modules = {
    // Scan for all auto bound modules
    val autoBoundModuleClasses = serviceScan.getReflections.getTypesAnnotatedWith(classOf[AutoBindModule]).asScala
    validateModules(autoBoundModuleClasses)

    // Instantiate all auto bound modules using the bootstrap injector
    val moduleListBuilder = new ModuleListBuilder
    val autoBoundModules = autoBoundModuleClasses.map(autoBoundModuleClazz => {
      val moduleProvider = new moduleListBuilder.ModuleProvider(autoBoundModuleClazz.asInstanceOf[Class[Module]])
      info(s"Auto-binding Guice module during bootstrap: $autoBoundModuleClazz")
      moduleProvider.getInstance(bootstrapInjector)
    })

    // Return both static and auto-bound modules to Finatra to be installed
    List(finatraPreBootstrapModule) ++ staticBootstrapModules ++ autoBoundModules
  }

  private[finatra] def validateModules(moduleClasses: Iterable[Class[_]]): Unit = {
    val invalidModules = moduleClasses
      .filter(moduleClazz => moduleClazz.isInterface || !classOf[Module].isAssignableFrom(moduleClazz))
    checkState(invalidModules.isEmpty, "@AutoBindModule can only annotate Module classes: %s", invalidModules)
  }
}
