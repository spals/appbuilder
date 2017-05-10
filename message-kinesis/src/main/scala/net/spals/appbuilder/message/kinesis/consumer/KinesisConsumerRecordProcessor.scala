package net.spals.appbuilder.message.kinesis.consumer

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import net.spals.appbuilder.config.message.MessageConsumerConfig
import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback
import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback.unregisteredCallbackMessage
import net.spals.appbuilder.model.core.ModelSerializer
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * A [[IRecordProcessor]] implementation
  * which deserializes messages and then calls
  * a [[MessageConsumerCallback]]
  *
  * @author tkral
  */
private[consumer] class KinesisConsumerRecordProcessor @Inject()
  (@Assisted consumerCallbacks: Map[Class[_], MessageConsumerCallback[_]],
   @Assisted consumerConfig: MessageConsumerConfig,
   @Assisted modelSerializer: ModelSerializer)
  extends IRecordProcessor {

  private val LOGGER = LoggerFactory.getLogger(classOf[KinesisConsumerRecordProcessor])

  override def initialize(initializationInput: InitializationInput): Unit = ()

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    val records = processRecordsInput.getRecords
    records.asScala.foreach(record => {
      val deserializedPayload = modelSerializer.deserialize(record.getData.array())
      val consumerCallback = consumerCallbacks.get(deserializedPayload.getClass)
      consumerCallback match {
        case Some(callback) =>
          callback.asInstanceOf[MessageConsumerCallback[AnyRef]].processMessage(consumerConfig, deserializedPayload)
        case None => LOGGER.warn(unregisteredCallbackMessage(consumerConfig.getTag, deserializedPayload.getClass))
      }

      LOGGER.trace(s"Checkpointing record ${record.getSequenceNumber} on partition ${record.getPartitionKey}")
      processRecordsInput.getCheckpointer().checkpoint(record)
    })
  }

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    shutdownInput.getShutdownReason match {
      case ShutdownReason.TERMINATE => shutdownInput.getCheckpointer.checkpoint()
      case _ => ()
    }
  }
}
