package net.spals.appbuilder.app.finatra

import com.google.inject.{Injector, Module}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Lifecycle
import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigResolveOptions}
import net.spals.appbuilder.app.core.modules.{AutoBindConfigModule, AutoBindServiceGraphModule, AutoBindServicesModule}
import net.spals.appbuilder.app.{core => spals}
import net.spals.appbuilder.graph.model.{ServiceGraph, ServiceGraphFormat}
import net.spals.appbuilder.graph.writer.ServiceGraphWriter
import org.reflections.Reflections
import org.slf4j

import scala.collection.JavaConverters._

/**
  * @author tkral
  */
trait FinatraWebApp extends HttpServer
  with Logging
  with spals.App
  with spals.WebAppBuilder[FinatraWebApp] {

  private lazy val flagConfig = {
    val flagMap = flag.getAll().map(flag => (flag.name, flag.apply())).toMap
    ConfigFactory.parseMap(flagMap.asJava)
  }

  // ========== Twitter HttpServer ==========

  override protected def configureHttp(router: HttpRouter): Unit = {
    webServerModule.runWebServerAutoBind(router)
  }

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()
    val serviceGraphWriter = injector.instance[ServiceGraphWriter]
    serviceGraphWriter.writeServiceGraph()
  }

  // ========== Spals App ==========

  override def getLogger: slf4j.Logger = logger.asInstanceOf[slf4j.Logger]

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
    modules ++ Seq(module)
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

    this
  }
}
