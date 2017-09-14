package net.spals.appbuilder.app.finatra.plugins

import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan
import net.spals.appbuilder.filestore.core.FileStore
import net.spals.appbuilder.mapstore.core.MapStore
import net.spals.appbuilder.message.core.{MessageConsumer, MessageProducer}
import net.spals.appbuilder.model.core.ModelSerializer

/**
  * A [[FinatraWebApp]] which uses all various service plugins.
  *
  * @author tkral
  */
object PluginsFinatraWebAppMain extends PluginsFinatraWebApp

private[finatra] class PluginsFinatraWebApp extends FinatraWebApp {

  setServiceConfigFromClasspath("config/plugins-finatra-service.conf")
  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.finatra.plugins")
    .addServicePlugins("net.spals.appbuilder.filestore.s3", classOf[FileStore])
    .addServicePlugins("net.spals.appbuilder.mapstore.cassandra", classOf[MapStore])
    .addServicePlugins("net.spals.appbuilder.mapstore.dynamodb", classOf[MapStore])
    .addServicePlugins("net.spals.appbuilder.message.kafka", classOf[MessageConsumer], classOf[MessageProducer])
    .addServicePlugins("net.spals.appbuilder.message.kinesis", classOf[MessageConsumer], classOf[MessageProducer])
    .addServicePlugins("net.spals.appbuilder.model.protobuf", classOf[ModelSerializer])
    .build())
  build()

}
