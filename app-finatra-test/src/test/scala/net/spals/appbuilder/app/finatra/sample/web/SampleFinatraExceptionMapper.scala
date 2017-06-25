package net.spals.appbuilder.app.finatra.sample.web

import com.google.inject.Inject
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
  * A sample Finatra [[ExceptionMapper]]
  *
  * @author tkral
  */
@AutoBindSingleton
private[finatra] class SampleFinatraExceptionMapper @Inject() (
  response: ResponseBuilder
) extends ExceptionMapper[Throwable] {

  override def toResponse(request: Request, throwable: Throwable): Response = {
    response.internalServerError.response
  }
}
