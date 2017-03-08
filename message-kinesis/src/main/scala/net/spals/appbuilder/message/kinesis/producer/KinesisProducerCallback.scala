package net.spals.appbuilder.message.kinesis.producer

import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.google.common.util.concurrent.FutureCallback
import org.slf4j.LoggerFactory

/**
  * @author tkral
  */
private[producer] case class KinesisProducerCallback(producerConfig: MessageProducerConfig) extends FutureCallback[UserRecordResult] {

  private val LOGGER = LoggerFactory.getLogger(s"${classOf[KinesisMessageProducerPlugin].getName}[${producerConfig.getTag}]")

  override def onFailure(t: Throwable): Unit =
    LOGGER.error("Error occurred while sending message on kinesis", t)

  override def onSuccess(result: UserRecordResult): Unit =
    LOGGER.trace(s"Successfully send message on kafka: shardId=${result.getShardId},sequenceNumber=${result.getSequenceNumber}")
}
