package net.spals.appbuilder.app.examples.finatra.plugins

import io.opentracing.Tracer
import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan
import net.spals.appbuilder.filestore.core.FileStore
import net.spals.appbuilder.graph.model.ServiceGraphFormat
import net.spals.appbuilder.keystore.core.KeyStore
import net.spals.appbuilder.mapstore.core.MapStore
import net.spals.appbuilder.message.core.{MessageConsumer, MessageProducer}
import net.spals.appbuilder.model.core.ModelSerializer

/**
  * A [[FinatraWebApp]] which uses all various service plugins.
  *
  * @author tkral
  */
object PluginsFinatraWebAppMain extends PluginsFinatraWebApp

class PluginsFinatraWebApp extends FinatraWebApp {

  enableServiceGraph(ServiceGraphFormat.TEXT)
  setServiceConfigFromClasspath("config/plugins-finatra-service.conf")
  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.examples.finatra.plugins")
    .addServicePlugins("net.spals.appbuilder.filestore.s3", classOf[FileStore])
    .addDefaultServices(classOf[KeyStore])
    .addServicePlugins("net.spals.appbuilder.mapstore.cassandra", classOf[MapStore])
    .addServicePlugins("net.spals.appbuilder.mapstore.dynamodb", classOf[MapStore])
    .addServicePlugins("net.spals.appbuilder.mapstore.mongodb", classOf[MapStore])
    .addServicePlugins("net.spals.appbuilder.message.kafka", classOf[MessageConsumer], classOf[MessageProducer])
    .addServicePlugins("net.spals.appbuilder.model.protobuf", classOf[ModelSerializer])
    .addServicePlugins("net.spals.appbuilder.monitor.lightstep", classOf[Tracer])
    .build())
  build()

}
