package net.spals.appbuilder.message.kafka.producer

import net.spals.appbuilder.config.message.MessageProducerConfig

/**
  * @author tkral
  */
private[producer] case class KafkaProducerConfig(producerConfig: MessageProducerConfig) {

  def getClientId: String = producerConfig.getGlobalId

  def getTopic: String = producerConfig.getChannel
}
