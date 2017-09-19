package net.spals.appbuilder.app.finatra.monitor

import javax.ws.rs.core.MultivaluedHashMap

import com.twitter.finagle.Service
import com.twitter.finagle.SimpleFilter
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.util.Future
import com.twitter.util.Return
import com.twitter.util.Throw
import io.opentracing.Span
import io.opentracing.SpanContext
import io.opentracing.Tracer
import io.opentracing.contrib.jaxrs2.server.ServerHeadersExtractTextMap
import io.opentracing.propagation.Format
import io.opentracing.tag.Tags

import scala.collection.JavaConverters._

/**
  * A web [[com.twitter.finagle.Filter]] which
  * automatically activates request tracing
  * using an OpenTracing [[Tracer]] instance.
  *
  * This is modeled off of [[io.opentracing.contrib.jaxrs2.server.ServerTracingFilter]]
  *
  * NOTE: This is not to be confused with a
  * Finagle [[com.twitter.finagle.tracing.Tracer]]
  * and the related infrastructure.
  */
private[finatra] class FinatraTracingFilter (
  tracer: Tracer
) extends SimpleFilter[Request, Response] {

  override def apply(
    request: Request,
    service: Service[Request, Response]
  ): Future[Response] = {
    val headerMap = new MultivaluedHashMap[String, String](request.headerMap.asJava)
    val extractedSpanContext: SpanContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
      new ServerHeadersExtractTextMap(headerMap))

    val spanBuilder = tracer.buildSpan(createOperationName(request))
      .withTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_SERVER)
      .withTag(Tags.HTTP_METHOD.getKey, request.method.name)
    // Add all param key, value pairs as tags
    request.params.foreach(param => spanBuilder.withTag(s"param.${param._1}", s"${param._2}"))

    Option(extractedSpanContext).foreach(spanBuilder.asChildOf(_))

    val span = spanBuilder.startManual()
    tracer.makeActive(span)

    service(request).respond {
      case Return(response) =>
        Tags.HTTP_STATUS.set(span, response.statusCode)
        finishSpan(span)
      case Throw(e) =>
        Tags.HTTP_STATUS.set(span, Status.InternalServerError.code)
        finishSpan(span)
    }
  }

  private[finatra] def createOperationName(request: Request): String = {
    // Replace all param values with param keys
    request.params.toList.foldRight(request.path) { (param, path) =>
      path.replaceAll(param._2, s":${param._1}")
    }
  }

  private[finatra] def finishSpan(span: Span): Unit = {
    Option(tracer.activeSpan()) match {
      case Some(activeSpan) => activeSpan.deactivate()
      case None => span.finish()
    }
  }
}
