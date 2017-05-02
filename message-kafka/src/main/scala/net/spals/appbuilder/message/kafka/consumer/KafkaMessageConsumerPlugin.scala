package net.spals.appbuilder.message.kafka.consumer

import java.util.Properties
import java.util.concurrent.Executors

import com.google.inject.Inject
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.config.message.MessageConsumerConfig
import net.spals.appbuilder.executor.core.ManagedExecutorServiceRegistry
import net.spals.appbuilder.message.core.consumer.{MessageConsumerCallback, MessageConsumerPlugin}
import net.spals.appbuilder.message.core.formatter.MessageFormatter
import org.apache.kafka.clients.consumer.ConsumerConfig._
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * A [[MessageConsumerPlugin]] for consuming messages
  * from a Kafka queue.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MessageConsumerPlugin], key = "kafka")
private[consumer] class KafkaMessageConsumerPlugin @Inject()
  (consumerCallbackSet: java.util.Set[MessageConsumerCallback[_]],
   executorServiceRegistry: ManagedExecutorServiceRegistry)
  extends MessageConsumerPlugin {

  @Configuration("kafka.messageConsumer.bootstrapServers")
  private var bootstrapServers: String = null

  @Configuration("kafka.messageConsumer.numThreads")
  private var numThreads: Int = 2

  private val consumerRunnableCache = mutable.Map[MessageConsumerConfig, KafkaConsumerRunnable]()

  private[consumer] def createConsumerProps(kafkaConsumerConfig: KafkaConsumerConfig): Properties = {
    val props = new Properties()
    props.put(AUTO_COMMIT_INTERVAL_MS_CONFIG, Long.box(1000L))
    props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ENABLE_AUTO_COMMIT_CONFIG, Boolean.box(true))
    props.put(GROUP_ID_CONFIG, kafkaConsumerConfig.getGroupId)
    props.put(KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(SESSION_TIMEOUT_MS_CONFIG, Long.box(30000L))
    props.put(VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer].getName)

    props
  }

  override def start(consumerConfig: MessageConsumerConfig, messageFormatter: MessageFormatter): Unit = {
    val kafkaConsumerConfig = KafkaConsumerConfig(consumerConfig)

    val consumerProps = createConsumerProps(kafkaConsumerConfig)
    val consumer = new KafkaConsumer[String, Array[Byte]](consumerProps)
    consumer.subscribe(List(kafkaConsumerConfig.getTopic).asJava)

    val consumerRunnable = new KafkaConsumerRunnable(consumer,
      consumerCallbacks = loadCallbacks(consumerConfig),
      consumerConfig, messageFormatter)
    consumerRunnableCache ++= Map(consumerConfig -> consumerRunnable)

    val executorService = executorServiceRegistry.registerExecutorService(getClass,
      Executors.newFixedThreadPool(numThreads), consumerConfig.getTag)
    executorService.submit(consumerRunnable)
  }

  override def stop(consumerConfig: MessageConsumerConfig): Unit = {
    // Shutdown the native Kafka consumer within the KafkaConsumerRunnable
    consumerRunnableCache.get(consumerConfig).foreach(_.shutdown())
    // Stop the thread executor registered under the MessageConsumerConfig tag
    executorServiceRegistry.stop(getClass, consumerConfig.getTag)
  }

  private[kafka] def loadCallbacks(consumerConfig: MessageConsumerConfig): Map[Class[_], MessageConsumerCallback[_]] = {
    consumerCallbackSet.asScala.filter(_.getTag.equals(consumerConfig.getTag))
      .map(callback => (callback.getPayloadType, callback)).toMap
  }
}
