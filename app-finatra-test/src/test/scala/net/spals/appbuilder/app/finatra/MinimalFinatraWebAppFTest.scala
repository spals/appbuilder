package net.spals.appbuilder.app.finatra

import com.google.inject.Stage
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.EmbeddedHttpServer
import net.spals.appbuilder.app.finatra.minimal.MinimalFinatraWebApp
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

  @AfterClass def classTearDown(): Unit = {
    testServerWrapper.close()
  }

  @Test def testBasicFinatraWebApp() {
    assertThat(minimalApp.getName, is("net.spals.appbuilder.app.finatra.minimal.MinimalFinatraWebApp"))
    assertThat(minimalApp.getLogger, instanceOf[slf4j.Logger](classOf[slf4j.Logger]))
  }

  @DataProvider def serviceConfigProvider(): Array[Array[AnyRef]] = {
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

  @Test(dataProvider = "serviceConfigProvider")
  def testMinimalAppServiceConfig(configKey: String, expectedConfigValue: AnyRef) {
    val serviceConfig = minimalApp.getServiceConfig
    assertThat(serviceConfig.getAnyRef(configKey), is(expectedConfigValue))
  }

  @Test def testMinimalAppServiceInjector() {
    val serviceInjector = minimalApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[StatsReceiver]), notNullValue())
  }
}