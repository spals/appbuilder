package net.spals.appbuilder.filestore.s3

import java.util.Optional
import java.util.concurrent.ExecutorService
import javax.validation.constraints.{Min, NotNull}

import com.amazonaws.auth.AWSCredentialsProvider
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
import net.spals.appbuilder.executor.core.ExecutorServiceFactory.Key

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
  @Configuration("fileStore.s3.credentialsProvider")
  private[s3] var credentialsProviderClassName: String = null

  @NotNull
  @Configuration("fileStore.s3.endpoint")
  private[s3] var endpoint: String = null

  @Configuration("fileStore.s3.encryptionKey")
  private[s3] var encryptionKey: String = null

  override def get(): S3EncryptionHolder = {
    val s3Encryption = Option(encryptionKey).map(key => {

      val s3EncryptionBuilder = AmazonS3EncryptionClientBuilder.standard()
        .withCredentials(Class.forName(credentialsProviderClassName).newInstance.asInstanceOf[AWSCredentialsProvider])
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
  @Configuration("fileStore.s3.numUploadThreads")
  private[s3] var numUploadThreads: Int = 10

  override def get(): S3TransferEncryptionHolder = {
    val s3TransferEncryption = s3EncryptionHolder.value.asScala.map(s3Encryption => {
      TransferManagerBuilder.standard()
        .withS3Client(s3Encryption)
        .withExecutorFactory(new ExecutorFactory() {
          override def newExecutor(): ExecutorService =
            executorServiceFactory.createFixedThreadPool(numUploadThreads, new Key.Builder(getClass).build())
        })
        .withShutDownThreadPools(false)
        .build()
    })

    S3TransferEncryptionHolder(s3TransferEncryption.asJava)
  }
}
