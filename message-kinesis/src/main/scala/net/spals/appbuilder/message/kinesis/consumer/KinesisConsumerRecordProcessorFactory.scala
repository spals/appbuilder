package net.spals.appbuilder.message.kinesis.consumer

import net.spals.appbuilder.annotations.service.AutoBindFactory
import net.spals.appbuilder.config.message.MessageConsumerConfig
import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback
import net.spals.appbuilder.message.core.formatter.MessageFormatter

/**
  * @author tkral
  */
@AutoBindFactory
trait KinesisConsumerRecordProcessorFactory {

  def createRecordProcessor(consumerCallback: MessageConsumerCallback,
                            consumerConfig: MessageConsumerConfig,
                            messageFormatter: MessageFormatter): KinesisConsumerRecordProcessor
}
