package net.spals.appbuilder.message.kafka.consumer

import java.util.concurrent.atomic.AtomicBoolean

import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback
import net.spals.appbuilder.message.core.formatter.MessageFormatter
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException

import scala.collection.JavaConverters._

/**
  * @author tkral
  */
private[consumer] class KafkaConsumerRunnable (consumer: KafkaConsumer[String, Array[Byte]],
                                               consumerCallback: MessageConsumerCallback,
                                               consumerConfig: MessageConsumerConfig,
                                               messageFormatter: MessageFormatter) extends Runnable {

  private val closed = new AtomicBoolean(false)

  override def run(): Unit = {
    try {
      while (!closed.get()) {
        val records = consumer.poll(500L)
        records.iterator().asScala.foreach(record => {
          val deserializedPayload = messageFormatter.deserializePayload(record.value())
          consumerCallback.processMessage(consumerConfig, deserializedPayload)
        })
      }
    } catch {
      case e: WakeupException =>
        // Ignore exception if closing
        if (!closed.get()) throw e

    } finally {
      consumer.close()
    }
  }

  // Shutdown hook which can be called from a separate thread
  def shutdown() {
    closed.set(true)
    consumer.wakeup()
  }
}
