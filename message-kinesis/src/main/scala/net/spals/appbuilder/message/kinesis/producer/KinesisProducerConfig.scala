package net.spals.appbuilder.message.kinesis.producer

import net.spals.appbuilder.config.message.MessageProducerConfig

/**
  * @author tkral
  */
private[producer] case class KinesisProducerConfig(producerConfig: MessageProducerConfig) {

  def getStreamName: String = producerConfig.getChannel

}
