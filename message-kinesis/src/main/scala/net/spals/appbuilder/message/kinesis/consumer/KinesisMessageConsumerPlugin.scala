package net.spals.appbuilder.message.kinesis.consumer

import java.util.UUID
import javax.validation.constraints.{Min, NotNull}

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials, BasicSessionCredentials}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.google.inject.Inject
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.config.message.MessageConsumerConfig
import net.spals.appbuilder.executor.core.ExecutorServiceFactory
import net.spals.appbuilder.executor.core.ExecutorServiceFactory.Key
import net.spals.appbuilder.message.core.MessageConsumerCallback
import net.spals.appbuilder.message.core.MessageConsumerCallback.loadCallbacksForTag
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin
import net.spals.appbuilder.model.core.ModelSerializer

import scala.collection.JavaConverters._

/**
  * A [[MessageConsumerPlugin]] for consuming messages
  * from a Kinesis queue.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MessageConsumerPlugin], key = "kinesis")
private[consumer] class KinesisMessageConsumerPlugin @Inject()
  (@ApplicationName applicationName: String,
   consumerCallbackSet: java.util.Set[MessageConsumerCallback[_]],
   executorServiceFactory: ExecutorServiceFactory,
   kinesisConsumerRecordProcessorFactory: KinesisConsumerRecordProcessorFactory)
  extends MessageConsumerPlugin {

  @NotNull
  @Configuration("messageConsumer.kinesis.awsAccessKeyId")
  private var awsAccessKeyId: String = null

  @NotNull
  @Configuration("messageConsumer.kinesis.awsSecretKey")
  private var awsSecretKey: String = null

  @Configuration("messageConsumer.kinesis.awsSessionToken")
  private var awsSessionToken: String = null

  @NotNull
  @Configuration("messageConsumer.kinesis.endpoint")
  private var endpoint: String = null

  @Min(2L)
  @Configuration("messageConsumer.kinesis.numThreads")
  private var numThreads: Int = 2

  override def start(consumerConfig: MessageConsumerConfig, modelSerializer: ModelSerializer): Unit = {
    val kinesisConsumerConfig = KinesisConsumerConfig(consumerConfig)
    val consumerCallbacks = loadCallbacksForTag(consumerConfig.getTag, consumerCallbackSet).asScala.toMap

    val awsCredentials = Option(awsSessionToken)
      .map(sessionToken => new BasicSessionCredentials(awsAccessKeyId, awsSecretKey, sessionToken))
      .getOrElse(new BasicAWSCredentials(awsAccessKeyId, awsSecretKey))
    val workerId = s"${kinesisConsumerConfig.getWorkerId}:${UUID.randomUUID()}"

    val worker = new Worker.Builder()
      .config(new KinesisClientLibConfiguration(applicationName, kinesisConsumerConfig.getStreamName,
        new AWSStaticCredentialsProvider(awsCredentials), workerId))
      .recordProcessorFactory(new IRecordProcessorFactory() {
        override def createProcessor(): IRecordProcessor =
          kinesisConsumerRecordProcessorFactory.createRecordProcessor(consumerCallbacks, consumerConfig, modelSerializer)
      })
      .build()

      val executorService = executorServiceFactory.createFixedThreadPool(numThreads,
        new Key.Builder(getClass).addTags(consumerConfig.getTag).build())
      executorService.submit(worker)
  }

  override def stop(consumerConfig: MessageConsumerConfig): Unit = ()
}
