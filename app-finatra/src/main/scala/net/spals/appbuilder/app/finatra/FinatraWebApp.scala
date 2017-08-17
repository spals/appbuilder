package net.spals.appbuilder.app.finatra

import java.time.temporal.ChronoUnit

import com.google.inject.{Injector, Module}
import com.netflix.governator.guice.transformer.OverrideAllDuplicateBindings
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Lifecycle
import com.twitter.util.StorageUnit
import com.typesafe.config._
import net.spals.appbuilder.app.core.modules.{AutoBindConfigModule, AutoBindServiceGraphModule, AutoBindServicesModule}
import net.spals.appbuilder.app.finatra.bootstrap.FinatraBootstrapModule
import net.spals.appbuilder.app.finatra.modules.{AutoBindConfigFlagsModule, FinatraWebServerModule}
import net.spals.appbuilder.app.{core => spals}
import net.spals.appbuilder.config.service.ServiceScan
import net.spals.appbuilder.graph.model.{ServiceGraph, ServiceGraphFormat}
import net.spals.appbuilder.graph.writer.ServiceGraphWriter
import org.slf4j
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
  * @author tkral
  */
trait FinatraWebApp extends HttpServer
  with Logging
  with spals.App
  with spals.WebAppBuilder[FinatraWebApp] {

  private val customModules = new ListBuffer[Module]

  private val EXCLUDED_FLAGS = Set(
    "log.async" // log.async is both a value and a prefix which breaks Typesafe config parsing
  )
  private lazy val flagConfig = {
    // Filter out all flags which don't have an assigned or default value
    val flagsWithValues = flag.getAll().filter(flag => {
        flag.parse()
        flag.getWithDefault.isDefined
      })
      // Filter out explicitly excluded flags
      .filter(flag => !EXCLUDED_FLAGS.contains(flag.name))
    // Convert flags to types parseable by ConfigFactory
    val flagValueMap = flagsWithValues.map(flag => flag.apply() match {
      case duration: com.twitter.util.Duration => (flag.name, java.time.Duration.of(duration.inNanoseconds, ChronoUnit.NANOS))
      case iterable: Iterable[_] => (flag.name, iterable.asJava)
      case storageUnit: StorageUnit => (flag.name, ConfigMemorySize.ofBytes(storageUnit.bytes))
      case _ => (flag.name, Try(ConfigValueFactory.fromAnyRef(flag.apply())).getOrElse(flag.apply().toString))
    }).toMap
    ConfigFactory.parseMap(flagValueMap.asJava)
  }

  // ========== Twitter HttpServer ==========

  override protected def configureHttp(router: HttpRouter): Unit = {
    webServerModule.runWebServerAutoBind(router)
  }

  override def modules = customModules

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()
    val serviceGraphWriter = injector.instance[ServiceGraphWriter]
    serviceGraphWriter.writeServiceGraph()
  }

  // ========== Spals App ==========

  override def getLogger: slf4j.Logger = LoggerFactory.getLogger(loggerName)

  override def getName: String = name

  override def getServiceConfig: Config = altConfig.getOrElse(ConfigFactory.empty())
    .withFallback(flagConfig).resolve()

  override def getServiceInjector: Injector = injector.underlying

  // ========== Spals AppBuilder ==========

  // Alternative configuration outside of Flags
  private var altConfig: Option[Config] = None
  private val serviceGraph = new ServiceGraph()

  private var bootstrapModule = new FinatraBootstrapModule()
  private val configModuleBuilder = new AutoBindConfigModule.Builder(getName)
  private val serviceGraphModuleBuilder = new AutoBindServiceGraphModule.Builder(serviceGraph)
  private val servicesModuleBuilder = new AutoBindServicesModule.Builder
  private var webServerModule = FinatraWebServerModule(serviceGraph)

  override def addModule(module: Module): FinatraWebApp = {
    customModules += module
    this
  }

  def disableCommonFilters(): FinatraWebApp = {
    webServerModule = webServerModule.copy(addCommonFilters = false)
    this
  }

  override def disableErrorOnServiceLeaks(): FinatraWebApp = {
    servicesModuleBuilder.setErrorOnServiceLeaks(false)
    this
  }

  override def disableWebServerAutoBinding(): FinatraWebApp = {
    webServerModule = webServerModule.copy(runWebServerAutoBinding = false)
    this
  }

  override def enableBindingOverrides(): FinatraWebApp = {
    bootstrapModule = bootstrapModule.copy(moduleTransformers = List(new OverrideAllDuplicateBindings))
    this
  }

  override def enableRequestScoping(): FinatraWebApp = {
    webServerModule = webServerModule.copy(addRequestScopeFilter = true)
    this
  }

  override def enableServiceGraph(graphFormat: ServiceGraphFormat) = {
    serviceGraphModuleBuilder.setGraphFormat(graphFormat)
    this
  }

  override def setServiceConfig(serviceConfig: Config): FinatraWebApp = {
    altConfig = Option(serviceConfig)
    this
  }

  override def setServiceConfigFromClasspath(serviceConfigFileName: String): FinatraWebApp = {
    setServiceConfig(ConfigFactory.load(serviceConfigFileName,
      ConfigParseOptions.defaults.setAllowMissing(false),
      ConfigResolveOptions.defaults))
  }

  override def setServiceScan(serviceScan: ServiceScan): FinatraWebApp = {
    bootstrapModule = bootstrapModule.copy(serviceScan = serviceScan)
    configModuleBuilder.setServiceScan(serviceScan)
    servicesModuleBuilder.setServiceScan(serviceScan)
    this
  }

  override def build(): FinatraWebApp = {
    bootstrapModule = bootstrapModule.copy(serviceConfig = getServiceConfig,
      staticBootstrapModules = List(
        configModuleBuilder
          .setServiceConfig(getServiceConfig)
          .build(),
        serviceGraphModuleBuilder
          .build()))

    addFrameworkModules(
      bootstrapModule,
      servicesModuleBuilder.build(),
      webServerModule
    )
    // Bind alternate config values under @Flag annotations
    altConfig.foreach(config =>
      addFrameworkModules(AutoBindConfigFlagsModule(config)))

    this
  }
}
