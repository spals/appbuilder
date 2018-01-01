package net.spals.appbuilder.mapstore.mongodb

import java.util
import java.util.Optional
import javax.annotation.PreDestroy

import com.google.inject.Inject
import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model._
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.mapstore.core.model.{MapQueryOptions, MapStoreIndexName, MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.core.{MapStore, MapStoreIndexPlugin}

/**
  * Implementation of [[MapStoreIndexPlugin]] which
  * uses MongoDB.
  *
  * NOTE: This implementation follows the semantics
  * of a [[MapStore]], not a document store. As such,
  * it intentionally removes or ignores some default
  * functionality provided by MongoDB. For example,
  * this implementation ignores MongoDB's text and
  * geo search capabilities.
  *
  * If an application developer wishes to use more
  * advanced features of MongoDB, they should inject
  * the [[MongoClient]] and [[MongoDatabase]] directly
  * into their own custom services.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MapStoreIndexPlugin], key = "mongoDB")
private[mongodb] class MongoDBMapStoreIndexPlugin @Inject()(
  mongoDatabase: MongoDatabase,
  mapStore: MapStore
) extends MapStoreIndexPlugin {

  @PreDestroy
  override def close(): Unit = ()

  override def createIndex(
    indexName: MapStoreIndexName,
    indexKey: MapStoreTableKey
  ): Boolean = {
    // From MongoDB: Currently only single field hashed index supported.
    val hashIndex = Indexes.hashed(indexKey.getHashField)

    val collection = mongoDatabase.getCollection(indexName.getTableName)
    val indexOptions = new IndexOptions().name(indexName.getIndexName)
    indexName.getIndexName.equals(collection.createIndex(hashIndex, indexOptions))
  }


  override def dropIndex(indexName: MapStoreIndexName): Boolean = {
    val collection = mongoDatabase.getCollection(indexName.getTableName)
    collection.dropIndex(indexName.getIndexName)
    true
  }

  override def getItem(
    indexName: MapStoreIndexName,
    key: MapStoreKey
  ): Optional[util.Map[String, AnyRef]] = {
    mapStore.getItem(indexName.getTableName, key)
  }

  override def getItems(
    indexName: MapStoreIndexName,
    key: MapStoreKey,
    options: MapQueryOptions
  ): util.List[util.Map[String, AnyRef]] = {
    mapStore.getItems(indexName.getTableName, key, options)
  }
}
