package net.spals.appbuilder.app.examples.finatra.tracing

import com.google.inject.{Binder, Module}
import io.opentracing.Tracer
import io.opentracing.mock.MockTracer
import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan

/**
  * A [[FinatraWebApp]] which uses request tracing.
  *
  * @author tkral
  */
object TracingFinatraWebAppMain extends TracingFinatraWebApp

class TracingFinatraWebApp extends FinatraWebApp {

  val mockTracer = new MockTracer()

  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.examples.finatra.tracing")
    .build())
  addModule(new Module {
    override def configure(binder: Binder): Unit =
      binder.bind(classOf[Tracer]).toInstance(mockTracer)
  })
  enableBindingOverrides()
  build()

}
