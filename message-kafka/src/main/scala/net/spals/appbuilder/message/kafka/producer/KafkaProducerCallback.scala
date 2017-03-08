package net.spals.appbuilder.message.kafka.producer

import net.spals.appbuilder.config.ProducerConfig
import org.apache.kafka.clients.producer.{Callback, RecordMetadata}
import org.slf4j.LoggerFactory

/**
  * @author tkral
  */
private[producer] case class KafkaProducerCallback(producerConfig: ProducerConfig) extends Callback {

  private val LOGGER = LoggerFactory.getLogger(s"${classOf[KafkaMessageProducerPlugin].getName}[${producerConfig.getTag}]")

  override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
    (metadata, exception) match {
      case (_, null) => LOGGER.trace(s"Successfully send message on kafka: partition=${metadata.partition()},offset=${metadata.offset()}")
      case (null, _) => LOGGER.error("Error occurred while sending message on kafka", exception)
    }
  }
}
