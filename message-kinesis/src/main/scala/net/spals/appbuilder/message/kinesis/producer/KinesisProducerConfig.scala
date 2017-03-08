package net.spals.appbuilder.message.kinesis.producer

import net.spals.appbuilder.config.ProducerConfig

/**
  * @author tkral
  */
private[producer] case class KinesisProducerConfig(producerConfig: ProducerConfig) {

  def getStreamName: String = producerConfig.getChannel

}
