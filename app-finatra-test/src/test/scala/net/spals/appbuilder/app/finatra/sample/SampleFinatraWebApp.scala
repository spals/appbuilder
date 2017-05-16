package net.spals.appbuilder.app.finatra.sample

import net.spals.appbuilder.app.finatra.FinatraWebApp
import org.reflections.Reflections

class SampleFinatraWebApp extends FinatraWebApp {

  setServiceConfigFromClasspath("config/sample-finatra-service.conf")
  setServiceScan(new Reflections("net.spals.appbuilder.app.finatra.sample",
    "net.spals.appbuilder.executor.core",
    "net.spals.appbuilder.filestore.core",
    "net.spals.appbuilder.mapstore.core",
    "net.spals.appbuilder.message.core",
    "net.spals.appbuilder.model.core"))
  addModule(new SampleGuiceModule)
  addModule(new SampleTwitterModule)
  build()

}


