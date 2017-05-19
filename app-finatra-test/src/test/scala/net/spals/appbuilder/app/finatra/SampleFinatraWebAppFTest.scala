package net.spals.appbuilder.app.finatra

import com.google.inject.name.Names
import com.google.inject.{Key, Stage, TypeLiteral}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.annotations.FlagImpl
import net.spals.appbuilder.app.finatra.sample.{SampleCustomService, SampleFinatraWebApp}
import net.spals.appbuilder.executor.core.ManagedExecutorServiceRegistry
import net.spals.appbuilder.filestore.core.FileStore
import net.spals.appbuilder.mapstore.core.{MapStore, MapStorePlugin}
import net.spals.appbuilder.message.core.{MessageConsumer, MessageConsumerCallback, MessageProducer}
import net.spals.appbuilder.model.core.ModelSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.{hasKey, is, notNullValue}
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
      Array("fileStore.system", "localFS"),
      Array("mapStore.system", "mapDB")
    )
  }

  @Test(dataProvider = "serviceConfigProvider")
  def testServiceConfig(configKey: String, expectedConfigValue: AnyRef) {
    val serviceConfig = sampleApp.getServiceConfig
    assertThat(serviceConfig.getAnyRef(configKey), is(expectedConfigValue))
  }

  @Test(dataProvider = "serviceConfigProvider")
  def testServiceConfigInjection(configKey: String, expectedConfigValue: AnyRef) {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(Key.get(classOf[String], new FlagImpl(configKey))),
      is(String.valueOf(expectedConfigValue)))
  }

  @DataProvider def customModuleInjectionProvider(): Array[Array[AnyRef]] = {
    Array(
      Array("AutoBoundModule",
        "net.spals.appbuilder.app.finatra.sample.SampleFinatraWebApp:net.spals.appbuilder.app.finatra.sample.SampleAutoBoundModule"),
      Array("GuiceModule", "net.spals.appbuilder.app.finatra.sample.SampleGuiceModule"),
      Array("TwitterModule", "net.spals.appbuilder.app.finatra.sample.SampleTwitterModule")
    )
  }

  @Test(dataProvider = "customModuleInjectionProvider")
  def testCustomModuleInjection(keyName: String, expectedBindValue: String) {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(Key.get(classOf[String], Names.named(keyName))),
      is(expectedBindValue))
  }

  @Test def testCustomServiceInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[SampleCustomService]), notNullValue())
  }

  @Test def testExecutorInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[ManagedExecutorServiceRegistry]), notNullValue())
  }

  @Test def testFileStoreInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[FileStore]), notNullValue())
  }

  @Test def testMapStoreInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MapStore]), notNullValue())

    val mapStorePluginMapKey = new TypeLiteral[java.util.Map[String, MapStorePlugin]](){}
    val mapStorePluginMap = serviceInjector.getInstance(Key.get(mapStorePluginMapKey))
    assertThat(mapStorePluginMap, Matchers.aMapWithSize[String, MapStorePlugin](1))
    assertThat(mapStorePluginMap, hasKey("mapDB"))
  }

  @Test def testMessageInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MessageProducer]), notNullValue())
    assertThat(serviceInjector.getInstance(classOf[MessageConsumer]), notNullValue())

    val messageCallbackSetKey = new TypeLiteral[java.util.Set[MessageConsumerCallback[_]]](){}
    val messageCallbackSet = serviceInjector.getInstance(Key.get(messageCallbackSetKey))
    assertThat(messageCallbackSet, notNullValue())
  }

  @Test def testModelInjection() {
    val serviceInjector = sampleApp.getServiceInjector

    val modelSerializerMapKey = new TypeLiteral[java.util.Map[String, ModelSerializer]](){}
    val modelSerializerMap = serviceInjector.getInstance(Key.get(modelSerializerMapKey))
    assertThat(modelSerializerMap, Matchers.aMapWithSize[String, ModelSerializer](1))
    assertThat(modelSerializerMap, hasKey("pojo"))
  }
}
