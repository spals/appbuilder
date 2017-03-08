package net.spals.appbuilder.message.kafka.producer

import java.util.Properties

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin
import org.apache.kafka.clients.producer.ProducerConfig._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

/**
  * A [[MessageProducerPlugin]] for producing messages
  * to a Kafka queue.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MessageProducerPlugin], key = "kafka")
private[producer] class KafkaMessageProducerPlugin extends MessageProducerPlugin {

  @Configuration("kafka.messageProducer.bootstrapServers")
  private var bootstrapServers: String = null

  @Configuration("kafka.messageProducer.retries")
  private var retries: Int = 0

  private val producerCache = CacheBuilder.newBuilder()
    .build(new CacheLoader[KafkaProducerConfig, KafkaProducer[String, Array[Byte]]] {
      override def load(kafkaProducerConfig: KafkaProducerConfig): KafkaProducer[String, Array[Byte]] = {
        new KafkaProducer[String, Array[Byte]](createProducerProps(kafkaProducerConfig))
      }

      private def createProducerProps(kafkaProducerConfig: KafkaProducerConfig): Properties = {
        val props = new Properties()
        props.put(ACKS_CONFIG, "all")
        props.put(BATCH_SIZE_CONFIG, Int.box(16384))
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        props.put(BUFFER_MEMORY_CONFIG, Long.box(33554432))
        props.put(CLIENT_ID_CONFIG, kafkaProducerConfig.getClientId)
        props.put(KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
        props.put(LINGER_MS_CONFIG, Int.box(1))
        props.put(RETRIES_CONFIG, Int.box(retries))
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer].getName)

        props
      }
    })

  override def sendMessage(producerConfig: MessageProducerConfig, serializedPayload: Array[Byte]): Unit = {
    val kafkaProducerConfig = KafkaProducerConfig(producerConfig)
    val producer = producerCache.getUnchecked(kafkaProducerConfig)

    val producerRecord = new ProducerRecord[String, Array[Byte]](kafkaProducerConfig.getTopic, serializedPayload)
    producer.send(producerRecord, KafkaProducerCallback(producerConfig))
  }
}
