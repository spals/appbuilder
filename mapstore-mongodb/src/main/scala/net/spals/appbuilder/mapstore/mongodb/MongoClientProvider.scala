package net.spals.appbuilder.mapstore.mongodb

import javax.validation.constraints.NotNull

import com.google.inject.{Inject, Provider}
import com.mongodb.{MongoClient, MongoClientURI}
import com.netflix.governator.annotations.Configuration
import com.typesafe.config.ConfigException
import io.opentracing.Tracer
import io.opentracing.contrib.mongo.TracingMongoClient
import net.spals.appbuilder.annotations.service.AutoBindProvider

/**
  * A [[Provider]] of the [[MongoClient]].
  *
  * @author tkral
  */
@AutoBindProvider
private[mongodb] class MongoClientProvider @Inject() (
  tracer: Tracer
) extends Provider[MongoClient] {

  @Configuration("mapStore.mongoDB.host")
  private[mongodb] var host: String = null

  @NotNull
  @Configuration("mapStore.mongoDB.port")
  private[mongodb] var port: Int = 27017

  @Configuration("mapStore.mongoDB.url")
  private[mongodb] var url: String = null

  override def get(): MongoClient = {
    val mongoURL = Option(url).getOrElse(
      s"mongodb://${Option(host).getOrElse(throw new ConfigException.Missing("mapStore.mongoDB.host"))}:${port}")
    val mongoClientURI = new MongoClientURI(mongoURL)

    new TracingMongoClient(tracer, mongoClientURI)
  }
}
