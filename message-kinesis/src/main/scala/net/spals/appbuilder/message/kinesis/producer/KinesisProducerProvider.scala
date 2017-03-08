package net.spals.appbuilder.message.kinesis.producer

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration}
import com.google.inject.Provider
import com.netflix.governator.annotations.Configuration
import com.typesafe.config.ConfigException
import net.spals.appbuilder.annotations.service.AutoBindProvider

import scala.util.Try

/**
  * A [[Provider]] of the AWS [[KinesisProducer]].
  *
  * @author tkral
  */
@AutoBindProvider
private[producer] class KinesisProducerProvider() extends Provider[KinesisProducer] {

  @Configuration("kinesis.messageProducer.awsAccessKeyId")
  private var awsAccessKeyId: String = null

  @Configuration("kinesis.messageProducer.awsSecretKey")
  private var awsSecretKey: String = null

  @Configuration("kinesis.messageProducer.endpoint")
  private var endpoint: String = null

  override def get(): KinesisProducer = {
    val awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey)

    val config = new KinesisProducerConfiguration()
      .setCredentialsProvider(new AWSStaticCredentialsProvider(awsCredentials))

    endpoint match {
      case httpEndpoint if httpEndpoint.startsWith("http://") =>
        config.setKinesisEndpoint(httpEndpoint)
      case regionEndpoint if Try(Regions.fromName(regionEndpoint)).isSuccess =>
        config.setRegion(regionEndpoint)
      case _ => throw new ConfigException.BadValue("kinesis.messageProducer.endpoint",
        s"Unrecognized Kinesis endpoint. Value is neither an http endpoint nor a known region: $endpoint")
    }

    new KinesisProducer(config)
  }
}
