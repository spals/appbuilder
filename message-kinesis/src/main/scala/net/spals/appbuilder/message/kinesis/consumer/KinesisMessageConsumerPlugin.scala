package net.spals.appbuilder.message.kinesis.consumer

import java.util.UUID
import java.util.concurrent.Executors

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.google.inject.Inject
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.config.message.MessageConsumerConfig
import net.spals.appbuilder.executor.core.ManagedExecutorServiceRegistry
import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback.loadCallbacksForTag
import net.spals.appbuilder.message.core.consumer.{MessageConsumerCallback, MessageConsumerPlugin}
import net.spals.appbuilder.message.core.formatter.MessageFormatter

import scala.collection.JavaConverters._

/**
  * @author tkral
  */
private[consumer] class KinesisMessageConsumerPlugin @Inject()
  (@ApplicationName applicationName: String,
   consumerCallbackSet: java.util.Set[MessageConsumerCallback[_]],
   executorServiceRegistry: ManagedExecutorServiceRegistry,
   kinesisConsumerRecordProcessorFactory: KinesisConsumerRecordProcessorFactory)
  extends MessageConsumerPlugin {

  @Configuration("kinesis.messageConsumer.awsAccessKeyId")
  private var awsAccessKeyId: String = null

  @Configuration("kinesis.messageConsumer.awsSecretKey")
  private var awsSecretKey: String = null

  @Configuration("kinesis.messageConsumer.endpoint")
  private var endpoint: String = null

  @Configuration("kinesis.messageConsumer.numThreads")
  private var numThreads: Int = 2

  override def start(consumerConfig: MessageConsumerConfig, messageFormatter: MessageFormatter): Unit = {
    val kinesisConsumerConfig = KinesisConsumerConfig(consumerConfig)
    val consumerCallbacks = loadCallbacksForTag(consumerConfig.getTag, consumerCallbackSet).asScala.toMap

    val awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey)
    val workerId = s"${kinesisConsumerConfig.getWorkerId}:${UUID.randomUUID()}"

    val worker = new Worker.Builder()
      .config(new KinesisClientLibConfiguration(applicationName, kinesisConsumerConfig.getStreamName,
        new AWSStaticCredentialsProvider(awsCredentials), workerId))
      .recordProcessorFactory(new IRecordProcessorFactory() {
        override def createProcessor(): IRecordProcessor =
          kinesisConsumerRecordProcessorFactory.createRecordProcessor(consumerCallbacks, consumerConfig, messageFormatter)
      })
      .build()

      val executorService = executorServiceRegistry.registerExecutorService(getClass,
        Executors.newFixedThreadPool(numThreads), consumerConfig.getTag)
      executorService.submit(worker)
  }

  override def stop(consumerConfig: MessageConsumerConfig): Unit = {
    // Stop the thread executor registered under the MessageConsumerConfig tag
    executorServiceRegistry.stop(getClass, consumerConfig.getTag)
  }
}
