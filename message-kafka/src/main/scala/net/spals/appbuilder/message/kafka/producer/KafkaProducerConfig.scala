package net.spals.appbuilder.message.kafka.producer

import net.spals.appbuilder.config.ProducerConfig

/**
  * @author tkral
  */
private[producer] case class KafkaProducerConfig(producerConfig: ProducerConfig) {

  def getClientId: String = producerConfig.getGlobalId

  def getTopic: String = producerConfig.getChannel
}
