package net.spals.appbuilder.app.finatra

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.datastax.driver.core.Cluster
import com.google.inject.{Key, Stage, TypeLiteral}
import com.twitter.finatra.http.EmbeddedHttpServer
import net.spals.appbuilder.app.finatra.plugins.PluginsFinatraWebApp
import net.spals.appbuilder.filestore.core.{FileStore, FileStorePlugin}
import net.spals.appbuilder.keystore.core.{KeyStore, KeyStorePlugin}
import net.spals.appbuilder.mapstore.core.{MapStore, MapStoreIndex, MapStoreIndexPlugin, MapStorePlugin}
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin
import net.spals.appbuilder.message.core.{MessageConsumer, MessageProducer}
import net.spals.appbuilder.model.core.ModelSerializer
import net.spals.appbuilder.monitor.core.{TracerPlugin, TracerTag}
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.{hasKey, is, notNullValue}
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

  @Test def testFileStoreInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[FileStore]), notNullValue())

    val fileStorePluginMapKey = new TypeLiteral[java.util.Map[String, FileStorePlugin]](){}
    val fileStorePluginMap = serviceInjector.getInstance(Key.get(fileStorePluginMapKey))
    assertThat(fileStorePluginMap, Matchers.aMapWithSize[String, FileStorePlugin](2))
    assertThat(fileStorePluginMap, hasKey("localFS"))
    assertThat(fileStorePluginMap, hasKey("s3"))
  }

  @Test def testKeyStoreInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[KeyStore]), notNullValue())

    val keyStorePluginMapKey = new TypeLiteral[java.util.Map[String, KeyStorePlugin]]() {}
    val keyStorePluginMap = serviceInjector.getInstance(Key.get(keyStorePluginMapKey))
    assertThat(keyStorePluginMap, Matchers.aMapWithSize[String, KeyStorePlugin](1))
    assertThat(keyStorePluginMap, hasKey("password"))
  }

  @Test def testMapStoreInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MapStore]), notNullValue())

    val mapStorePluginMapKey = new TypeLiteral[java.util.Map[String, MapStorePlugin]](){}
    val mapStorePluginMap = serviceInjector.getInstance(Key.get(mapStorePluginMapKey))
    assertThat(mapStorePluginMap, Matchers.aMapWithSize[String, MapStorePlugin](4))
    assertThat(mapStorePluginMap, hasKey("cassandra"))
    assertThat(mapStorePluginMap, hasKey("dynamoDB"))
    assertThat(mapStorePluginMap, hasKey("mapDB"))
    assertThat(mapStorePluginMap, hasKey("mongoDB"))
  }

  @Test def testMapStoreIndexInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MapStoreIndex]), notNullValue())

    val mapStoreIndexPluginMapKey = new TypeLiteral[java.util.Map[String, MapStoreIndexPlugin]](){}
    val mapStoreIndexPluginMap = serviceInjector.getInstance(Key.get(mapStoreIndexPluginMapKey))
    assertThat(mapStoreIndexPluginMap, Matchers.aMapWithSize[String, MapStoreIndexPlugin](3))
    assertThat(mapStoreIndexPluginMap, hasKey("dynamoDB"))
    assertThat(mapStoreIndexPluginMap, hasKey("mapDB"))
    assertThat(mapStoreIndexPluginMap, hasKey("mongoDB"))
  }

  @Test def testCassandraMapStoreInjection() {
    val serviceInjector = pluginsApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[Cluster]), notNullValue())
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

  @Test def testTracerInjection() {
    val serviceInjector = pluginsApp.getServiceInjector

    val tracerPluginMapKey = new TypeLiteral[java.util.Map[String, TracerPlugin]](){}
    val tracerPluginMap = serviceInjector.getInstance(Key.get(tracerPluginMapKey))
    assertThat(tracerPluginMap, Matchers.aMapWithSize[String, TracerPlugin](2))
    assertThat(tracerPluginMap, hasKey("lightstep"))
    assertThat(tracerPluginMap, hasKey("noop"))
  }

  @Test def testTracerTagInjection() {
    val serviceInjector = pluginsApp.getServiceInjector

    val tracerTagMapKey = new TypeLiteral[java.util.Map[String, TracerTag]](){}
    val tracerTagMap = serviceInjector.getInstance(Key.get(tracerTagMapKey))
    assertThat(tracerTagMap, Matchers.aMapWithSize[String, TracerTag](2))
    assertThat(tracerTagMap, Matchers.hasEntry[String, TracerTag](is("key1"),
      is(new TracerTag.Builder().setTag("key1").setValue("value").build())))
    assertThat(tracerTagMap, Matchers.hasEntry[String, TracerTag](is("key2"),
      is(new TracerTag.Builder().setTag("key2").setValue(Int.box(2)).build())))
  }
}
