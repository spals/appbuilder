package net.spals.appbuilder.message.kinesis.consumer

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback
import net.spals.appbuilder.message.core.formatter.MessageFormatter
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
  (@Assisted consumerCallback: MessageConsumerCallback,
   @Assisted consumerConfig: MessageConsumerConfig,
   @Assisted messageFormatter: MessageFormatter)
  extends IRecordProcessor {

  private val LOGGER = LoggerFactory.getLogger(classOf[KinesisConsumerRecordProcessor])

  override def initialize(initializationInput: InitializationInput): Unit = ()

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    val records = processRecordsInput.getRecords
    records.asScala.foreach(record => {
      val deserializedPayload = messageFormatter.deserializePayload(record.getData.array())
      consumerCallback.processMessage(consumerConfig, deserializedPayload)

      LOGGER.trace(s"Checkpointing record ${record.getSequenceNumber} on partition ${record.getPartitionKey}")
      processRecordsInput.getCheckpointer().checkpoint(record)
    })
  }

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    shutdownInput.getShutdownReason match {
      case ShutdownReason.TERMINATE => shutdownInput.getCheckpointer.checkpoint()
    }
  }
}
