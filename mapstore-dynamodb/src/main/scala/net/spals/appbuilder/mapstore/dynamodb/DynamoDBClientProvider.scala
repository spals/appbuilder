package net.spals.appbuilder.mapstore.dynamodb

import javax.validation.constraints.NotNull

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.google.inject.{Inject, Provider}
import com.netflix.governator.annotations.Configuration
import com.typesafe.config.ConfigException
import io.opentracing.Tracer
import io.opentracing.contrib.aws.TracingRequestHandler
import net.spals.appbuilder.annotations.service.AutoBindProvider

import scala.util.Try

/**
  * A [[Provider]] of the AWS [[DynamoDB]]
  * Document API object.
  *
  * @author tkral
  */
@AutoBindProvider
private[dynamodb] class DynamoDBClientProvider @Inject() (
  tracer: Tracer
) extends Provider[AmazonDynamoDB] {

  @NotNull
  @Configuration("mapStore.dynamoDB.awsAccessKeyId")
  private[dynamodb] var awsAccessKeyId: String = null

  @NotNull
  @Configuration("mapStore.dynamoDB.awsSecretKey")
  private[dynamodb] var awsSecretKey: String = null

  @NotNull
  @Configuration("mapStore.dynamoDB.endpoint")
  private[dynamodb] var endpoint: String = null

  override def get(): AmazonDynamoDB = {
    val awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey)
    val dynamoDBClientBuilder = AmazonDynamoDBClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
      .withRequestHandlers(new TracingRequestHandler(tracer))

    endpoint match {
      case httpEndpoint if httpEndpoint.startsWith("http://") => {
        val endpointConfig = new EndpointConfiguration(httpEndpoint, null)
        dynamoDBClientBuilder.withEndpointConfiguration(endpointConfig)
      }
      case regionEndpoint if Try(Regions.fromName(regionEndpoint)).isSuccess => dynamoDBClientBuilder.withRegion(regionEndpoint)
      case _ => throw new ConfigException.BadValue("mapStore.dynamoDB.endpoint",
        s"Unrecognized DynamoDB endpoint. Value is neither an http endpoint nor a known region: $endpoint")
    }

    dynamoDBClientBuilder.build()
  }
}
