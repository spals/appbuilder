package net.spals.appbuilder.message.kinesis.consumer

/**
  * @author tkral
  */
private[consumer] case class KinesisConsumerConfig(consumerConfig: MessageConsumerConfig) {

  def getStreamName: String = consumerConfig.getChannel

  def getWorkerId: String = consumerConfig.getGlobalId
}
