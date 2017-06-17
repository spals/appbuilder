package net.spals.appbuilder.message.kafka

import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}

import com.google.common.collect.ImmutableSet
import net.spals.appbuilder.config.message.{MessageConsumerConfig, MessageProducerConfig}
import net.spals.appbuilder.executor.core.ExecutorServiceFactory
import net.spals.appbuilder.message.core.TestMessageConsumerCallback
import net.spals.appbuilder.message.kafka.consumer.KafkaMessageConsumerPlugin
import net.spals.appbuilder.message.kafka.producer.KafkaMessageProducerPlugin
import net.spals.appbuilder.model.core.ModelSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.is
import org.mockito.ArgumentMatchers.{any, anyInt, anyString}
import org.mockito.Mockito.{mock, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.testng.annotations.Test

/**
  * Integration tests for the Kafka message system.
  *
  * @author tkral
  */
class KafkaMessageIT {

  private lazy val executorServiceFactory = {
    val executorServiceFactory = mock(classOf[ExecutorServiceFactory])
    when(executorServiceFactory.createFixedThreadPool(anyInt, any(classOf[java.lang.Class[_]]), anyString))
      .thenReturn(Executors.newSingleThreadExecutor)

    executorServiceFactory
  }

  private lazy val modelSerializer = {
    val modelSerializer = mock(classOf[ModelSerializer])
    when(modelSerializer.serialize(any(classOf[AnyRef])))
      .thenAnswer(new Answer[Array[Byte]] {
        override def answer(invocationOnMock: InvocationOnMock): Array[Byte] =
          invocationOnMock.getArgument(0).toString.getBytes
      })
    when(modelSerializer.deserialize(any(classOf[Array[Byte]])))
      .thenAnswer(new Answer[String] {
        override def answer(invocationOnMock: InvocationOnMock): String =
          new String(invocationOnMock.getArgument(0).asInstanceOf[Array[Byte]])
      })

    modelSerializer
  }

  private val kafkaBootstrapServers = s"${System.getenv("KAFKA_IP")}:${System.getenv("KAFKA_PORT")}"

  private lazy val producerPlugin = {
    val producerPlugin = new KafkaMessageProducerPlugin()
    producerPlugin.bootstrapServers = kafkaBootstrapServers

    producerPlugin
  }

  private val producerConfig = new MessageProducerConfig.Builder()
    .setTag("myTag").setGlobalId("myProducerId").setFormat("pojo")
    .setDestination("kafka").setChannel("myChannel").build
  private val consumerConfig = new MessageConsumerConfig.Builder()
    .setTag("myTag").setGlobalId("myConsumerId").setFormat("pojo")
    .setSource("kafka").setChannel("myChannel").build

  @Test(enabled = false) def testKafkaMessage() {
    val cdl = new CountDownLatch(1)
    val consumerCallback = new TestMessageConsumerCallback(cdl, "payload")

    val consumerPlugin = new KafkaMessageConsumerPlugin(ImmutableSet.of(consumerCallback), executorServiceFactory)
    consumerPlugin.bootstrapServers = kafkaBootstrapServers
    consumerPlugin.start(consumerConfig, modelSerializer)

    producerPlugin.sendMessage(producerConfig, "payload".getBytes)
    assertThat(cdl.await(5L, TimeUnit.SECONDS), is(true))
  }
}
