package net.spals.appbuilder.app.finatra.sample

import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan
import net.spals.appbuilder.executor.core.ManagedExecutorServiceRegistry
import net.spals.appbuilder.filestore.core.FileStore
import net.spals.appbuilder.mapstore.core.MapStore
import net.spals.appbuilder.message.core.{MessageConsumer, MessageProducer}
import net.spals.appbuilder.model.core.ModelSerializer

class SampleFinatraWebApp extends FinatraWebApp {

  setServiceConfigFromClasspath("config/sample-finatra-service.conf")
  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.finatra.sample")
    .addDefaultServices(classOf[ManagedExecutorServiceRegistry])
    .addDefaultServices(classOf[FileStore])
    .addDefaultServices(classOf[MapStore])
    .addDefaultServices(classOf[MessageConsumer], classOf[MessageProducer])
    .addDefaultServices(classOf[ModelSerializer])
    .build())
  addModule(new SampleGuiceModule)
  addModule(new SampleTwitterModule)
  build()

}


