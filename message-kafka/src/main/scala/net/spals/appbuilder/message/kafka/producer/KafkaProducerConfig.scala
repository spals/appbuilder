package net.spals.appbuilder.message.kafka.producer

/**
  * @author tkral
  */
private[producer] case class KafkaProducerConfig(producerConfig: MessageProducerConfig) {

  def getClientId: String = producerConfig.getGlobalId

  def getTopic: String = producerConfig.getChannel
}
