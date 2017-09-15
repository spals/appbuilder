package net.spals.appbuilder.app.finatra

import com.github.mustachejava.MustacheFactory
import com.google.inject._
import com.googlecode.catchexception.CatchException.verifyException
import com.googlecode.catchexception.ThrowingCallable
import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.exceptions.ExceptionManager
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyReader, DefaultMessageBodyWriter}
import com.twitter.finatra.http.response.ResponseBuilder
import io.opentracing.Tracer
import net.spals.appbuilder.app.finatra.minimal.MinimalFinatraWebApp
import net.spals.appbuilder.executor.core.ExecutorServiceFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{instanceOf, is, notNullValue}
import org.slf4j
import org.testng.annotations.{AfterClass, BeforeClass, DataProvider, Test}
/**
  * Functional tests for a minimal [[FinatraWebApp]]
  *
  * @author tkral
  */
class MinimalFinatraWebAppFTest {

  private val minimalApp = new MinimalFinatraWebApp()
  private val testServerWrapper = new EmbeddedHttpServer(
    twitterServer = minimalApp,
    stage = Stage.PRODUCTION
  )

  @BeforeClass def classSetup() {
    testServerWrapper.start()
  }

  @AfterClass def classTearDown() {
    testServerWrapper.close()
  }

  @Test def testFinatraWebAppLogger() {
    assertThat(minimalApp.getLogger, instanceOf[slf4j.Logger](classOf[slf4j.Logger]))
  }

  @Test def testFinatraWebAppName() {
    assertThat(minimalApp.getName, is("net.spals.appbuilder.app.finatra.minimal.MinimalFinatraWebApp"))
  }

  @DataProvider def defaultServiceConfigProvider(): Array[Array[AnyRef]] = {
    Array(
      Array("http.port", ":8888"),
      Array("log.append", Boolean.box(true)),
      // Case: Convert non-parseable types to Strings
      Array("log.level", "INFO"),
      // Case: Convert memory sizes
      Array("maxRequestSize", Long.box(5242880L)),
      // Case: Convert durations
      Array("shutdown.time", Long.box(60000L))
    )
  }

  @Test(dataProvider = "defaultServiceConfigProvider")
  def testDefaultServiceConfig(configKey: String, expectedConfigValue: AnyRef) {
    val serviceConfig = minimalApp.getServiceConfig
    assertThat(serviceConfig.getAnyRef(configKey), is(expectedConfigValue))
  }

  @DataProvider
  def defaultServiceInjectionProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(TypeLiteral.get(classOf[DefaultMessageBodyReader])),
      Array(TypeLiteral.get(classOf[DefaultMessageBodyWriter])),
      Array(TypeLiteral.get(classOf[ExceptionManager])),
      Array(TypeLiteral.get(classOf[MustacheFactory])),
      Array(new TypeLiteral[LogFormatter[Request, Response]](){}),
      Array(TypeLiteral.get(classOf[ResponseBuilder])),
      Array(TypeLiteral.get(classOf[StatsReceiver]))
    )
  }

  @Test(dataProvider = "defaultServiceInjectionProvider")
  def testDefaultServiceInjectoion(typeLiteral: TypeLiteral[_]) {
    val serviceInjector = minimalApp.getServiceInjector
    val service = serviceInjector.getInstance(Key.get(typeLiteral)).asInstanceOf[AnyRef]
    assertThat(service, notNullValue())
  }

  @DataProvider
  def noDefaultServiceInjectionProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(TypeLiteral.get(classOf[ExecutorServiceFactory])),
      Array(TypeLiteral.get(classOf[Tracer]))
    )
  }

  @Test(dataProvider = "noDefaultServiceInjectionProvider")
  def testNoDefaultServiceInjection(typeLiteral: TypeLiteral[_]): Unit = {
    val serviceInjector = minimalApp.getServiceInjector

    val serviceLookup = new ThrowingCallable {
      override def call(): Unit = serviceInjector.getInstance(Key.get(typeLiteral))
    }
    verifyException(serviceLookup, classOf[ConfigurationException])
  }
}
