package net.spals.appbuilder.app.finatra

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Stage
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import net.spals.appbuilder.app.finatra.doc.DocFinatraWebApp
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{hasEntry, hasKey, is, not}
import org.testng.annotations.{AfterClass, BeforeClass, Test}

import scala.collection.JavaConverters._

/**
  * Functional tests for API documentation in a [[FinatraWebApp]]
  * (see [[DocFinatraWebApp]]).
  */
class DocFinatraWebAppFTest {

  private val objectMapper = new ObjectMapper()

  private val docApp = new DocFinatraWebApp()
  private val testServerWrapper = new EmbeddedHttpServer(
    twitterServer = docApp,
    stage = Stage.PRODUCTION
  )

  @BeforeClass def classSetup() {
    testServerWrapper.start()
  }

  @AfterClass def classTearDown() {
    testServerWrapper.close()
  }

  @Test def testApiDocumentation() {
    val docResponse = testServerWrapper.httpGet("/api-docs/model")
    assertThat(docResponse.statusCode, is(Ok.code))

    val docContent = docResponse.contentString
    val json = objectMapper.readValue(docContent, new TypeReference[java.util.HashMap[String, AnyRef]] {})
      .asInstanceOf[java.util.HashMap[String, AnyRef]]

    assertThat(json, hasKey("info"))
    val info = json.get("info").asInstanceOf[java.util.Map[String, AnyRef]]
    assertThat(info, hasEntry[String, AnyRef]("title", "DocFinatraWebApp API"))

    assertThat(json, hasKey("paths"))
    val paths = json.get("paths").asInstanceOf[java.util.Map[String, AnyRef]]
    assertThat(paths, hasKey("/doc/get"))
    assertThat(paths, hasKey("/doc/get/{id}"))
  }

  @Test def testCorsDisabled() {
    val docResponse = testServerWrapper.httpGet("/doc/get")
    assertThat(docResponse.statusCode, is(Ok.code))

    val headerMap = docResponse.headerMap.asJava
    // Ensure that CORS is disabled on other endpoints outside of Swagger.
    assertThat(headerMap, not(hasKey("Access-Control-Allow-Origin")))
    assertThat(headerMap, not(hasKey("Access-Control-Allow-Credentials")))
    assertThat(headerMap, not(hasKey("Access-Control-Allow-Headers")))
    assertThat(headerMap, not(hasKey("Access-Control-Allow-Methods")))
  }
}
