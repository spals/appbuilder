package net.spals.appbuilder.app.finatra.sample.web

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.util.Future
import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
  * A sample Finstra [[Filter]]
  *
  * @author tkral
  */
@AutoBindSingleton
private[finatra] class SampleFinatraFilter extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    service.apply(request)
  }
}
