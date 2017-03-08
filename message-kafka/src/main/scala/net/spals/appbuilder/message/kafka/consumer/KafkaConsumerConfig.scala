package net.spals.appbuilder.message.kafka.consumer

/**
  * @author tkral
  */
private[consumer] case class KafkaConsumerConfig(consumerConfig: MessageConsumerConfig) {

  def getGroupId: String = consumerConfig.getGlobalId

  def getTopic: String = consumerConfig.getChannel
}
