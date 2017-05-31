package net.spals.appbuilder.filestore.s3

import javax.validation.constraints.NotNull

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.Provider
import com.netflix.governator.annotations.Configuration
import com.typesafe.config.ConfigException
import net.spals.appbuilder.annotations.service.AutoBindProvider

import scala.util.Try

/**
  * @author tkral
  */
@AutoBindProvider
private[s3] class S3ClientProvider extends Provider[AmazonS3] {

  @NotNull
  @Configuration("fileStore.s3.awsAccessKeyId")
  private var awsAccessKeyId: String = null

  @NotNull
  @Configuration("fileStore.s3.awsSecretKey")
  private var awsSecretKey: String = null

  @NotNull
  @Configuration("fileStore.s3.endpoint")
  private var endpoint: String = null

  override def get(): AmazonS3 = {
    val awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey)
    val s3ClientBuilder = AmazonS3ClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))

    endpoint match {
      case httpEndpoint if httpEndpoint.startsWith("http://") => {
        val endpointConfig = new EndpointConfiguration(httpEndpoint, null)
        s3ClientBuilder.withEndpointConfiguration(endpointConfig)
      }
      case regionEndpoint if Try(Regions.fromName(regionEndpoint)).isSuccess => s3ClientBuilder.withRegion(regionEndpoint)
      case _ => throw new ConfigException.BadValue("fileStore.s3.endpoint",
        s"Unrecognized S3 endpoint. Value is neither an http endpoint nor a known region: $endpoint")
    }

    s3ClientBuilder.build()
  }
}
