package net.spals.appbuilder.app.finatra.cors

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
  * A Finatra [[Controller]] used in CORS registration tests.
  *
  * @author tkral
  */
@AutoBindSingleton
private[cors] class CorsFinatraController extends Controller {

  get("/cors/get") { request: Request =>
    response.ok
  }
}
