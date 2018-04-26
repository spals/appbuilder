package net.spals.appbuilder.app.finatra

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Stage
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import net.spals.appbuilder.app.finatra.cors.CorsFinatraWebApp
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{hasEntry, is}
import org.testng.annotations.{AfterClass, BeforeClass, Test}

import scala.collection.JavaConverters._

/**
  * Functional tests for CORS registration in a [[FinatraWebApp]]
  * (see [[CorsFinatraWebApp]]).
  */
class CorsFinatraWebAppFTest {

  private val objectMapper = new ObjectMapper()

  private val corsApp = new CorsFinatraWebApp()
  private val testServerWrapper = new EmbeddedHttpServer(
    twitterServer = corsApp,
    stage = Stage.PRODUCTION
  )

  @BeforeClass def classSetup() {
    testServerWrapper.start()
  }

  @AfterClass def classTearDown() {
    testServerWrapper.close()
  }

  @Test def testCorsEnabled() {
    val docResponse = testServerWrapper.httpOptions(
      path = "/cors/get",
      headers = Map("Origin" -> "Some", "Access-Control-Request-Method" -> "GET")
    )
    assertThat(docResponse.statusCode, is(Ok.code))

    val headerMap = docResponse.headerMap.asJava
    assertThat(headerMap, hasEntry[String, String]("Access-Control-Allow-Origin", "*"))
    assertThat(headerMap, hasEntry[String, String]("Access-Control-Allow-Headers", "origin, content-type, accept, authorization"))
    assertThat(headerMap, hasEntry[String, String]("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD"))
  }
}
