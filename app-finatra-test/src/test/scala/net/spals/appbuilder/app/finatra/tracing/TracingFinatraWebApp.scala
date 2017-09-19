package net.spals.appbuilder.app.finatra.tracing

import com.google.inject.Binder
import com.google.inject.Module
import io.opentracing.Tracer
import io.opentracing.mock.MockTracer
import net.spals.appbuilder.app.finatra.FinatraWebApp
import net.spals.appbuilder.config.service.ServiceScan

/**
  * A [[FinatraWebApp]] which uses all various service plugins.
  *
  * @author tkral
  */
object TracingFinatraWebAppMain extends TracingFinatraWebApp

private[finatra] class TracingFinatraWebApp extends FinatraWebApp {

  val mockTracer = new MockTracer()

  setServiceScan(new ServiceScan.Builder()
    .addServicePackages("net.spals.appbuilder.app.finatra.tracing")
    .build())
  addModule(new Module {
    override def configure(binder: Binder): Unit =
      binder.bind(classOf[Tracer]).toInstance(mockTracer)
  })
  enableBindingOverrides()
  build()

}
