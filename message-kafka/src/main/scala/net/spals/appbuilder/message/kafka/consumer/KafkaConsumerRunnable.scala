package net.spals.appbuilder.message.kafka.consumer

import java.util.concurrent.atomic.AtomicBoolean

import net.spals.appbuilder.config.message.MessageConsumerConfig
import net.spals.appbuilder.message.core.MessageConsumerCallback
import net.spals.appbuilder.message.core.MessageConsumerCallback.unregisteredCallbackMessage
import net.spals.appbuilder.model.core.ModelSerializer
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
                                               modelSerializer: ModelSerializer) extends Runnable {

  private val LOGGER = LoggerFactory.getLogger(classOf[KafkaConsumerRunnable])
  private val closed = new AtomicBoolean(false)

  override def run(): Unit = {
    try {
      while (!closed.get()) {
        val records = consumer.poll(500L)
        records.iterator().asScala.foreach(record => {
          val deserializedPayload = modelSerializer.deserialize(record.value())
          val consumerCallback = consumerCallbacks.get(deserializedPayload.getClass)
          consumerCallback match {
            case Some(callback) =>
              callback.asInstanceOf[MessageConsumerCallback[AnyRef]].processMessage(consumerConfig, deserializedPayload)
            case None => LOGGER.warn(unregisteredCallbackMessage(consumerConfig.getTag, deserializedPayload.getClass))
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
