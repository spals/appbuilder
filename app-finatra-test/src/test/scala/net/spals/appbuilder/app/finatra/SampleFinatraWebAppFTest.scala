package net.spals.appbuilder.app.finatra

import com.google.inject.name.Names
import com.google.inject.{Key, Stage, TypeLiteral}
import com.twitter.finatra.http.EmbeddedHttpServer
import net.spals.appbuilder.app.finatra.sample.SampleFinatraWebApp
import net.spals.appbuilder.model.core.ModelSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{hasKey, is}
import org.testng.annotations.{AfterClass, BeforeClass, DataProvider, Test}

/**
  * Functional tests for a sample [[FinatraWebApp]]
  *
  * @author tkral
  */
class SampleFinatraWebAppFTest {

  private val sampleApp = new SampleFinatraWebApp()
  private val testServerWrapper = new EmbeddedHttpServer(
    twitterServer = sampleApp,
    stage = Stage.PRODUCTION
  )

  @BeforeClass def classSetup() {
    testServerWrapper.start()
  }

  @AfterClass def classTearDown(): Unit = {
    testServerWrapper.close()
  }

  @DataProvider def serviceConfigProvider(): Array[Array[AnyRef]] = {
    Array(
      Array("mapStore.system", "mapDB")
    )
  }

  @Test(dataProvider = "serviceConfigProvider")
  def testSampleAppServiceConfig(configKey: String, expectedConfigValue: AnyRef) {
    val serviceConfig = sampleApp.getServiceConfig
    assertThat(serviceConfig.getAnyRef(configKey), is(expectedConfigValue))
  }

  @Test(enabled = false) def testCustomModuleInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(Key.get(classOf[String], Names.named("GuiceModule"))),
      is("net.spals.appbuilder.app.finatra.sample.SampleGuiceModule"))
    assertThat(serviceInjector.getInstance(Key.get(classOf[String], Names.named("TwitterModule"))),
      is("net.spals.appbuilder.app.finatra.sample.SampleTwitterModule"))
  }

  @Test def testModelInjection() {
    val serviceInjector = sampleApp.getServiceInjector

    val modelSerializerMapKey = new TypeLiteral[java.util.Map[String, ModelSerializer]](){}
    val modelSerializerMap = serviceInjector.getInstance(Key.get(modelSerializerMapKey))
    assertThat(modelSerializerMap, hasKey("pojo"))
  }
}
