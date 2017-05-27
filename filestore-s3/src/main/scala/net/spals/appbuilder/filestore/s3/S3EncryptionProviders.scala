package net.spals.appbuilder.filestore.s3

import java.util.Optional
import java.util.concurrent.ExecutorService
import javax.validation.constraints.{Min, NotNull}

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider
import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.amazonaws.services.s3.{AmazonS3Encryption, AmazonS3EncryptionClientBuilder}
import com.google.inject.{Inject, Provider}
import com.netflix.governator.annotations.Configuration
import com.typesafe.config.ConfigException
import net.spals.appbuilder.annotations.service.AutoBindProvider
import net.spals.appbuilder.executor.core.ExecutorServiceFactory

import scala.compat.java8.OptionConverters._
import scala.util.Try

/**
  * A wrapper around an [[AmazonS3Encryption]] object which
  * allows the object to be undefined.
  *
  * Consumers should check the value before using it.
  *
  * @author tkral
  */
case class S3EncryptionHolder(value: Optional[AmazonS3Encryption])

/**
  * Guice [[Provider]] for [[S3EncryptionHolder]].
  *
  * @author tkral
  */
@AutoBindProvider
private[s3] class S3EncryptionHolderProvider extends Provider[S3EncryptionHolder] {

  @NotNull
  @Configuration("s3.fileStore.awsAccessKeyId")
  private var awsAccessKeyId: String = null

  @NotNull
  @Configuration("s3.fileStore.awsSecretKey")
  private var awsSecretKey: String = null

  @NotNull
  @Configuration("s3.fileStore.endpoint")
  private var endpoint: String = null

  @Configuration("s3.fileStore.encryptionKey")
  private var encryptionKey: String = null


  override def get(): S3EncryptionHolder = {
    val s3Encryption = Option(encryptionKey).map(key => {
      val awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey)

      val s3EncryptionBuilder = AmazonS3EncryptionClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withEncryptionMaterials(new KMSEncryptionMaterialsProvider(key))

      endpoint match {
        case httpEndpoint if httpEndpoint.startsWith("http://") => {
          val endpointConfig = new EndpointConfiguration(httpEndpoint, null)
          s3EncryptionBuilder.withEndpointConfiguration(endpointConfig)
        }
        case regionEndpoint if Try(Regions.fromName(regionEndpoint)).isSuccess => s3EncryptionBuilder.withRegion(regionEndpoint)
        case _ => throw new ConfigException.BadValue("s3.fileStore.endpoint",
          s"Unrecognized S3 endpoint. Value is neither an http endpoint nor a known region: $endpoint")
      }

      s3EncryptionBuilder.build()
    })

    S3EncryptionHolder(s3Encryption.asJava)
  }
}

/**
  * A wrapper around a [[TransferManager]] object which
  * allows the object to be undefined. If present, the
  * [[TransferManager]] will be enabled with encryption
  * capabilities.
  *
  * Consumers should check the value before using it.
  *
  * @author tkral
  */
case class S3TransferEncryptionHolder(value: Optional[TransferManager])

/**
  * Guice [[Provider]] for [[S3TransferEncryptionHolder]].
  *
  * @author tkral
  */
@AutoBindProvider
private[s3] class S3TransferEncryptionHolderProvider @Inject() (
  s3EncryptionHolder: S3EncryptionHolder,
  executorServiceFactory: ExecutorServiceFactory
) extends Provider[S3TransferEncryptionHolder] {

  @Min(2)
  @Configuration("s3.fileStore.numUploadThreads")
  private var numUploadThreads: Int = 10

  override def get(): S3TransferEncryptionHolder = {
    val s3TransferEncryption = s3EncryptionHolder.value.asScala.map(s3Encryption => {
      TransferManagerBuilder.standard()
        .withS3Client(s3Encryption)
        .withExecutorFactory(new ExecutorFactory() {
          override def newExecutor(): ExecutorService =
            executorServiceFactory.createFixedThreadPool(numUploadThreads, getClass)
        })
        .withShutDownThreadPools(false)
        .build()
    })

    S3TransferEncryptionHolder(s3TransferEncryption.asJava)
  }
}
