package net.spals.appbuilder.app.finatra.tracing

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
  * A Finatra [[Controller]] used in request tracing tests.
  *
  * @author tkral
  */
@AutoBindSingleton
private[tracing] class TracingFinatraController extends Controller {

  get("/tracing") { request: Request =>
    response.ok
  }

  get("/tracing/:id") { request: Request =>
    response.ok
  }
}
