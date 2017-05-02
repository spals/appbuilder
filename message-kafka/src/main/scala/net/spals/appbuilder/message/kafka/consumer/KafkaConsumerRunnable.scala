package net.spals.appbuilder.message.kafka.consumer

import java.util.concurrent.atomic.AtomicBoolean

import net.spals.appbuilder.config.message.MessageConsumerConfig
import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback
import net.spals.appbuilder.message.core.formatter.MessageFormatter
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * @author tkral
  */
private[consumer] class KafkaConsumerRunnable (consumer: KafkaConsumer[String, Array[Byte]],
                                               consumerCallbacks: Map[Class[_], MessageConsumerCallback[_]],
                                               consumerConfig: MessageConsumerConfig,
                                               messageFormatter: MessageFormatter) extends Runnable {

  private val LOGGER = LoggerFactory.getLogger(classOf[KafkaConsumerRunnable])
  private val closed = new AtomicBoolean(false)

  override def run(): Unit = {
    try {
      while (!closed.get()) {
        val records = consumer.poll(500L)
        records.iterator().asScala.foreach(record => {
          val deserializedPayload = messageFormatter.deserializePayload(record.value())
          val consumerCallback = consumerCallbacks.get(deserializedPayload.getClass)
          consumerCallback match {
            case Some(callback) => callback.processMessage(consumerConfig, deserializedPayload)
            case None => LOGGER.warn(s"Received payload type ${deserializedPayload.getClass} for consumer ${consumerConfig.getTag}, but no callback is registered")
          }
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
