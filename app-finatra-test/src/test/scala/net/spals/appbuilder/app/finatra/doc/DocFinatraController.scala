package net.spals.appbuilder.app.finatra.doc

import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
  * A Finatra [[Controller]] used in API documentation tests.
  *
  * @author tkral
  */
@AutoBindSingleton
private[doc] class DocFinatraController extends Controller with SwaggerSupport {
  implicit protected val swagger = FinatraWebAppSwagger

  getWithDoc("/doc/get") { apiDoc =>
    apiDoc.summary("Test for API documentation with no parameters")
  } { request: Request =>
    response.ok
  }

  getWithDoc("/doc/get/:id") { apiDoc =>
    apiDoc.summary("Test for API documentation with single parameter")
  }{ request: Request =>
    response.ok
  }
}
