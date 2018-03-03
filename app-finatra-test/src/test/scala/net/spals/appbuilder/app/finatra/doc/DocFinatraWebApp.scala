package net.spals.appbuilder.app.finatra.doc

import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan

/**
  * A [[FinatraWebApp]] which uses API documentation.
  *
  * @author tkral
  */
object DocFinatraWebAppMain extends DocFinatraWebApp

private[finatra] class DocFinatraWebApp extends FinatraWebApp {

  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.finatra.doc")
    .build())
  build()

}
