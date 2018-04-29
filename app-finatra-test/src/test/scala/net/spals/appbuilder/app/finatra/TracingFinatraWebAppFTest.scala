package net.spals.appbuilder.app.finatra

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import io.opentracing.mock.MockSpan
import net.spals.appbuilder.app.examples.finatra.tracing.TracingFinatraWebApp
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.is
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
  * Functional tests for request tracing in a [[FinatraWebApp]]
  * (see [[TracingFinatraWebApp]]).
  */
class TracingFinatraWebAppFTest {

  private val tracingApp = new TracingFinatraWebApp()
  private val testServerWrapper = new EmbeddedHttpServer(
    twitterServer = tracingApp,
    stage = Stage.PRODUCTION
  )
  private val mockTracer = tracingApp.mockTracer

  @BeforeClass def classSetup() {
    testServerWrapper.start()
  }

  @BeforeMethod def resetTracer() {
    mockTracer.reset()
  }

  @AfterClass def classTearDown() {
    testServerWrapper.close()
  }

  @Test def testServerRequestTracing() {
    testServerWrapper.httpGet("/tracing")

    val mockSpans = mockTracer.finishedSpans()
    assertThat("No finished spans found.", mockSpans, hasSize[MockSpan](1))

    val mockSpan = mockSpans.get(0)
    assertThat(mockSpan.generatedErrors, empty[RuntimeException]())
    assertThat(mockSpan.operationName(), is("/tracing"))
    assertThat(mockSpan.tags(), hasEntry[String, AnyRef]("http.method", "GET"))
    assertThat(mockSpan.tags(), hasEntry[String, AnyRef]("http.status_code", Int.box(200)))
    assertThat(mockSpan.tags(), hasEntry[String, AnyRef]("span.kind", "server"))
  }

  @Test def testServerRequestTracingWithParams() {
    testServerWrapper.httpGet("/tracing/123")

    val mockSpans = mockTracer.finishedSpans()
    assertThat("No finished spans found.", mockSpans, hasSize[MockSpan](1))

    val mockSpan = mockSpans.get(0)
    assertThat(mockSpan.generatedErrors, empty[RuntimeException]())
    assertThat(mockSpan.operationName(), is("/tracing/:id"))
    assertThat(mockSpan.tags(), hasEntry[String, AnyRef]("http.method", "GET"))
    assertThat(mockSpan.tags(), hasEntry[String, AnyRef]("http.status_code", Int.box(200)))
    assertThat(mockSpan.tags(), hasEntry[String, AnyRef]("param.id", "123"))
    assertThat(mockSpan.tags(), hasEntry[String, AnyRef]("span.kind", "server"))
  }
}
