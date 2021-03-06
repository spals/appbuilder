package net.spals.appbuilder.message.kafka.consumer

import net.spals.appbuilder.config.message.MessageConsumerConfig

/**
  * @author tkral
  */
private[consumer] case class KafkaConsumerConfig(consumerConfig: MessageConsumerConfig) {

  def getGroupId: String = consumerConfig.getGlobalId

  def getTopic: String = consumerConfig.getChannel
}
