package net.spals.appbuilder.app.finatra

import java.time.temporal.ChronoUnit

import com.google.inject.{Injector, Module}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Lifecycle
import com.twitter.util.StorageUnit
import com.typesafe.config._
import net.spals.appbuilder.app.core.modules.{AutoBindConfigModule, AutoBindServiceGraphModule, AutoBindServicesModule}
import net.spals.appbuilder.app.finatra.modules.AutoBindConfigFlagsModule
import net.spals.appbuilder.app.{core => spals}
import net.spals.appbuilder.graph.model.{ServiceGraph, ServiceGraphFormat}
import net.spals.appbuilder.graph.writer.ServiceGraphWriter
import org.reflections.Reflections
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

  private val configModuleBuilder = new AutoBindConfigModule.Builder
  private val serviceGraphModuleBuilder = new AutoBindServiceGraphModule.Builder
  private val servicesModuleBuilder = new AutoBindServicesModule.Builder
  private val webServerModule = FinatraWebServerModule(serviceGraph)

  override def addModule(module: Module): FinatraWebApp = {
    customModules += module
    this
  }

  def disableCommonFilters(): FinatraWebApp = {
    webServerModule.addCommonFilters.set(false)
    this
  }

  override def disableErrorOnServiceLeaks(): FinatraWebApp = {
    servicesModuleBuilder.setErrorOnServiceLeaks(false)
    this
  }

  override def disableWebServerAutoBinding(): FinatraWebApp = {
    webServerModule.runWebServerAutoBinding.set(false)
    this
  }

  override def enableRequestScoping(): FinatraWebApp = {
    webServerModule.addRequestScopeFilter.set(true)
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

  override def setServiceScan(serviceScan: Reflections): FinatraWebApp = {
    configModuleBuilder.setServiceScan(serviceScan)
    servicesModuleBuilder.setServiceScan(serviceScan)
    this
  }

  override def build(): FinatraWebApp = {
    addFrameworkModules(
      configModuleBuilder
        .setApplicationName(getName)
        .setServiceConfig(getServiceConfig)
        .build(),
      serviceGraphModuleBuilder
        .setServiceGraph(serviceGraph)
        .build(),
      servicesModuleBuilder.build(),
      webServerModule
    )
    // Bind alternate config values under @Flag annotations
    altConfig.foreach(config =>
      addFrameworkModules(AutoBindConfigFlagsModule(config)))


    this
  }
}
