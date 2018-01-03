package net.spals.appbuilder.mapstore.mongodb

import com.google.common.annotations.VisibleForTesting
import com.google.inject.{Inject, Provider}
import com.mongodb.client.MongoDatabase
import com.mongodb.{MongoClient, MongoClientURI}
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindProvider
import org.slf4j.LoggerFactory

/**
  * @author tkral
  */
@AutoBindProvider
private[mongodb] class MongoDatabaseProvider @Inject() (
  @ApplicationName applicationName: String,
  mongoClient: MongoClient
) extends Provider[MongoDatabase] {
  private val LOGGER = LoggerFactory.getLogger(classOf[MongoDatabaseProvider])

  @Configuration("mapStore.mongoDB.database")
  @volatile
  @VisibleForTesting
  private[mongodb] var configuredDatabaseName: String = null

  @Configuration("mapStore.mongoDB.url")
  @volatile
  @VisibleForTesting
  private[mongodb] var url: String = null

  @VisibleForTesting
  private[mongodb] lazy val databaseName = {
    val mongoClientURI = Option(url).map(new MongoClientURI(_))
    mongoClientURI.map(_.getDatabase)
      .getOrElse(Option(configuredDatabaseName).getOrElse(applicationName))
  }

  override def get(): MongoDatabase = {
    LOGGER.info(s"MongoDB database is: ${databaseName}")
    mongoClient.getDatabase(databaseName)
  }
}
