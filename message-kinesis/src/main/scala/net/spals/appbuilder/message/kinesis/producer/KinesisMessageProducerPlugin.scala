package net.spals.appbuilder.message.kinesis.producer

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.producer.KinesisProducer
import com.google.common.util.concurrent.Futures
import com.google.inject.Inject
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.config.message.MessageProducerConfig
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin

/**
  * A [[MessageProducerPlugin]] for producing messages
  * to a Kinesis queue.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MessageProducerPlugin], key = "kinesis")
private[producer] class KinesisMessageProducerPlugin @Inject() (@ApplicationName applicationName: String,
                                                                producer: KinesisProducer)
  extends MessageProducerPlugin {

  override def sendMessage(producerConfig: MessageProducerConfig, serializedPayload: Array[Byte]): Unit = {
    val kinesisProducerConfig = KinesisProducerConfig(producerConfig)
    val producerFuture = producer.addUserRecord(kinesisProducerConfig.getStreamName,
      applicationName /*partitionKey*/, ByteBuffer.wrap(serializedPayload))

    Futures.addCallback(producerFuture, new KinesisProducerCallback(producerConfig))
  }
}