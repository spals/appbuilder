package net.spals.appbuilder.message.kinesis.producer

/**
  * @author tkral
  */
private[producer] case class KinesisProducerConfig(producerConfig: MessageProducerConfig) {

  def getStreamName: String = producerConfig.getChannel

}
