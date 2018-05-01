package net.spals.appbuilder.app.examples.finatra.cors

import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan

/**
  * A [[FinatraWebApp]] which registers CORS.
  *
  * @author tkral
  */
object CorsFinatraWebAppMain extends CorsFinatraWebApp

class CorsFinatraWebApp extends FinatraWebApp {

  enableCors()
  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.finatra.cors")
    .build())
  build()

}
