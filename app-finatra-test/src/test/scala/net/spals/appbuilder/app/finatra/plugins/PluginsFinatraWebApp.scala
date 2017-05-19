package net.spals.appbuilder.app.finatra.plugins

import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan
import net.spals.appbuilder.mapstore.core.MapStore
import net.spals.appbuilder.model.core.ModelSerializer

/**
  * A [[FinatraWebApp]] which uses all various service plugins.
  *
  * @author tkral
  */
class PluginsFinatraWebApp extends FinatraWebApp {

  setServiceConfigFromClasspath("config/plugins-finatra-service.conf")
  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.finatra.plugins")
    .addServicePlugins("net.spals.appbuilder.mapstore.cassandra", classOf[MapStore])
    .addServicePlugins("net.spals.appbuilder.mapstore.dynamodb", classOf[MapStore])
    .addServicePlugins("net.spals.appbuilder.model.protobuf", classOf[ModelSerializer])
    .build())
  build()

}
