package net.spals.appbuilder.mapstore.mongodb

import javax.validation.constraints.NotNull

import com.google.inject.{Inject, Provider}
import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindProvider

/**
  * @author tkral
  */
@AutoBindProvider
private[mongodb] class MongoDatabaseProvider @Inject() (
  @ApplicationName applicationName: String,
  mongoClient: MongoClient
) extends Provider[MongoDatabase] {

  @NotNull
  @Configuration("mapStore.mongoDB.database")
  @volatile
  private[mongodb] var configuredDatabaseName: String = null

  private lazy val databaseName = Option(configuredDatabaseName).getOrElse(applicationName)

  override def get(): MongoDatabase = mongoClient.getDatabase(databaseName)
}
