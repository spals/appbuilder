package net.spals.appbuilder.message.kafka.consumer

import net.spals.appbuilder.config.ConsumerConfig

/**
  * @author tkral
  */
private[consumer] case class KafkaConsumerConfig(consumerConfig: ConsumerConfig) {

  def getGroupId: String = consumerConfig.getGlobalId

  def getTopic: String = consumerConfig.getChannel
}
