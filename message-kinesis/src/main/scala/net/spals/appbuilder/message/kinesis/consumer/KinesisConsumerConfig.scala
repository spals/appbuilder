package net.spals.appbuilder.message.kinesis.consumer

import net.spals.appbuilder.config.ConsumerConfig

/**
  * @author tkral
  */
private[consumer] case class KinesisConsumerConfig(consumerConfig: ConsumerConfig) {

  def getStreamName: String = consumerConfig.getChannel

  def getWorkerId: String = consumerConfig.getGlobalId
}
