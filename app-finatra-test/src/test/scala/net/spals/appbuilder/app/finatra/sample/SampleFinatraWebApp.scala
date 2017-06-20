package net.spals.appbuilder.app.finatra.sample

import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan
import net.spals.appbuilder.executor.core.ExecutorServiceFactory
import net.spals.appbuilder.filestore.core.FileStore
import net.spals.appbuilder.mapstore.core.MapStore
import net.spals.appbuilder.message.core.{MessageConsumer, MessageProducer}
import net.spals.appbuilder.model.core.ModelSerializer

/**
  * A full sample [[FinatraWebApp]] which uses all default services
  * and bindings.
  *
  * @author tkral
  */
class SampleFinatraWebApp extends FinatraWebApp {

  setServiceConfigFromClasspath("config/sample-finatra-service.conf")
  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.finatra.sample")
    .addDefaultServices(classOf[ExecutorServiceFactory])
    .addDefaultServices(classOf[FileStore])
    .addDefaultServices(classOf[MapStore])
    .addDefaultServices(classOf[MessageConsumer], classOf[MessageProducer])
    .addDefaultServices(classOf[ModelSerializer])
    .build())
  addModule(new SampleFinatraGuiceModule)
  addModule(new SampleFinatraTwitterModule)
  build()

}
