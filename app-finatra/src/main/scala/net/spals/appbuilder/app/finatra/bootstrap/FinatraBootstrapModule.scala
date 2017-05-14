package net.spals.appbuilder.app.finatra.bootstrap

import com.google.common.base.Preconditions.checkState
import com.google.common.base.Predicates
import com.google.inject.{AbstractModule, Module}
import com.netflix.governator.guice.{BootstrapModule, LifecycleInjector, ModuleListBuilder}
import com.twitter.inject.{Logging, TwitterModule}
import net.spals.appbuilder.annotations.service.AutoBindModule
import net.spals.appbuilder.app.core.bootstrap.{AutoBindModulesBootstrapModule, BootstrapModuleWrapper}
import org.reflections.Reflections

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
    staticBootstrapModules: Seq[Module] = List(),
    serviceScan: Reflections = new Reflections(Predicates.alwaysFalse())
  )
  extends TwitterModule
  with Logging {

  // Dynamic module to install all static bootstrap modules
  // (that is, any module which binds state that can feed @AutoBindModules)
  private val staticBootstrapInstallModule = new AbstractModule {
    override def configure(): Unit = {
      staticBootstrapModules.foreach(staticBootstrapModule => {
        info(s"Installing static bootstrap module: ${staticBootstrapModule.getClass}")
        install(staticBootstrapModule)
      })
    }
  }

  override lazy val modules = {
    // Use Governator to create an injector based on the static bootstrap modules
    val bootstrapInjector = LifecycleInjector.bootstrap(classOf[AnyRef] /*dummy*/,
      new BootstrapModuleWrapper(staticBootstrapInstallModule))

    // Scan for all auto bound modules
    val autoBoundModuleClasses = serviceScan.getTypesAnnotatedWith(classOf[AutoBindModule]).asScala
    validateModules(autoBoundModuleClasses)

    // Instantiate all auto bound modules using the bootstrap injector
    val moduleListBuilder = new ModuleListBuilder
    val autoBoundModules = autoBoundModuleClasses.map(autoBoundModuleClazz => {
      val moduleProvider = new moduleListBuilder.ModuleProvider(autoBoundModuleClazz.asInstanceOf[Class[Module]])
      info(s"Auto-binding Guice module during bootstrap: $autoBoundModuleClazz")
      moduleProvider.getInstance(bootstrapInjector)
    })

    // Return both static and auto-bound modules to Finatra to be installed
    staticBootstrapModules ++ autoBoundModules
  }

  private[finatra] def validateModules(moduleClasses: Iterable[Class[_]]): Unit = {
    val invalidModules = moduleClasses
      .filter(moduleClazz => moduleClazz.isInterface || !classOf[Module].isAssignableFrom(moduleClazz))
    checkState(invalidModules.isEmpty, "@AutoBindModule can only annotate Module classes: %s", invalidModules)
  }
}
