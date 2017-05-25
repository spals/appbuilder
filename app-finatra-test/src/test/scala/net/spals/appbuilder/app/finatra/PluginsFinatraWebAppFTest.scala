package net.spals.appbuilder.app.finatra

import java.util.concurrent.FutureTask

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.datastax.driver.core.Session
import com.google.inject.{Key, Stage, TypeLiteral}
import com.twitter.finatra.http.EmbeddedHttpServer
import net.spals.appbuilder.app.finatra.plugins.PluginsFinatraWebApp
import net.spals.appbuilder.mapstore.core.{MapStore, MapStorePlugin}
import net.spals.appbuilder.message.core.{MessageConsumer, MessageProducer}
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin
import net.spals.appbuilder.model.core.ModelSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.{hasKey, notNullValue}
import org.testng.annotations.{AfterClass, BeforeClass, Test}

/**
  * Functional tests for a plugins [[FinatraWebApp]]
  *
  * @author tkral
  */
class PluginsFinatraWebAppFTest {

  private val pluginsApp = new PluginsFinatraWebApp()
  private val testServerWrapper = new EmbeddedHttpServer(
    twitterServer = pluginsApp,
    stage = Stage.PRODUCTION
  )

  @BeforeClass def classSetup() {
    testServerWrapper.start()
  }

  @AfterClass def classTearDown(): Unit = {
    testServerWrapper.close()
  }

  @Test def testMapStoreInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MapStore]), notNullValue())

    val mapStorePluginMapKey = new TypeLiteral[java.util.Map[String, MapStorePlugin]](){}
    val mapStorePluginMap = serviceInjector.getInstance(Key.get(mapStorePluginMapKey))
    assertThat(mapStorePluginMap, Matchers.aMapWithSize[String, MapStorePlugin](3))
    assertThat(mapStorePluginMap, hasKey("cassandra"))
    assertThat(mapStorePluginMap, hasKey("dynamoDB"))
    assertThat(mapStorePluginMap, hasKey("mapDB"))

  }

  @Test def testCassandraMapStoreInjection() {
    val serviceInjector = pluginsApp.getServiceInjector

    val sessionFutureKey = new TypeLiteral[FutureTask[Session]](){}
    assertThat(serviceInjector.getInstance(Key.get(sessionFutureKey)), notNullValue())
  }

  @Test def testDynamoDBMapStoreInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[AmazonDynamoDB]), notNullValue())
  }

  @Test def testMessageConsumerInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MessageConsumer]), notNullValue())

    val messageConsumerPluginMapKey = new TypeLiteral[java.util.Map[String, MessageConsumerPlugin]](){}
    val messageConsumerPluginMap = serviceInjector.getInstance(Key.get(messageConsumerPluginMapKey))
    assertThat(messageConsumerPluginMap, Matchers.aMapWithSize[String, MessageConsumerPlugin](3))
    assertThat(messageConsumerPluginMap, hasKey("blockingQueue"))
    assertThat(messageConsumerPluginMap, hasKey("kafka"))
    assertThat(messageConsumerPluginMap, hasKey("kinesis"))
  }

  @Test def testMessageProducerInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MessageProducer]), notNullValue())

    val messageProducerPluginMapKey = new TypeLiteral[java.util.Map[String, MessageProducerPlugin]](){}
    val messageProducerPluginMap = serviceInjector.getInstance(Key.get(messageProducerPluginMapKey))
    assertThat(messageProducerPluginMap, Matchers.aMapWithSize[String, MessageProducerPlugin](3))
    assertThat(messageProducerPluginMap, hasKey("blockingQueue"))
    assertThat(messageProducerPluginMap, hasKey("kafka"))
    assertThat(messageProducerPluginMap, hasKey("kinesis"))
  }

  @Test def testModelInjection() {
    val serviceInjector = pluginsApp.getServiceInjector

    val modelSerializerMapKey = new TypeLiteral[java.util.Map[String, ModelSerializer]](){}
    val modelSerializerMap = serviceInjector.getInstance(Key.get(modelSerializerMapKey))
    assertThat(modelSerializerMap, Matchers.aMapWithSize[String, ModelSerializer](2))
    assertThat(modelSerializerMap, hasKey("pojo"))
    assertThat(modelSerializerMap, hasKey("protobuf"))
  }
}
