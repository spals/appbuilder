package net.spals.appbuilder.mapstore.mongodb

import java.io.Closeable
import java.util.Optional
import javax.annotation.PreDestroy

import com.google.common.annotations.VisibleForTesting
import com.google.inject.Inject
import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model._
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.mapstore.core.MapStorePlugin
import net.spals.appbuilder.mapstore.core.MapStorePlugin.stripKey
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.Order
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.{Extended, Standard}
import net.spals.appbuilder.mapstore.core.model.MultiValueMapRangeKey.ListValueHolder
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.TwoValueHolder
import net.spals.appbuilder.mapstore.core.model.{MapQueryOptions, MapStoreKey, MapStoreTableKey}
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory

import scala.compat.java8.OptionConverters._

/**
  * Implementation of [[MapStorePlugin]] which
  * uses MongoDB.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MapStorePlugin], key = "mongoDB")
private[mongodb] class MongoDBMapStorePlugin @Inject() (
  mongoDatabase: MongoDatabase,
  mongoClient: MongoClient
) extends MapStorePlugin with Closeable {
  private val LOGGER = LoggerFactory.getLogger(classOf[MongoDBMapStorePlugin])

  @PreDestroy
  override def close(): Unit = mongoClient.close()

  override def createTable(
    tableName: String,
    tableKey: MapStoreTableKey
  ): Boolean = {
    // Turn of auto-indexing as we'll create our own explicit index
    val collectionOptions = new CreateCollectionOptions().autoIndex(false)
    mongoDatabase.createCollection(tableName, collectionOptions)

    val hashIndex = Indexes.hashed(tableKey.getHashField)
    val fullIndex = tableKey.getRangeField.asScala
      .map(rangeField => Indexes.compoundIndex(hashIndex, Indexes.ascending(rangeField)))
      .getOrElse(hashIndex)

    val collection = mongoDatabase.getCollection(tableName)
    val indexOptions = new IndexOptions().name("primary")
    val indexName = collection.createIndex(fullIndex, indexOptions)

    "primary".equals(indexName)
  }

  override def dropTable(tableName: String): Boolean = {
    val collection = mongoDatabase.getCollection(tableName)
    collection.drop()

    true
  }

  override def deleteItem(
    tableName: String,
    key: MapStoreKey
  ): Unit = {
    val collection = mongoDatabase.getCollection(tableName)

    val filter = createFilter(key)
    collection.deleteOne(filter)
  }

  override def getAllItems(
    tableName: String
  ): java.util.List[java.util.Map[String, AnyRef]] = {
    val collection = mongoDatabase.getCollection(tableName)

    val allItems = new java.util.ArrayList[java.util.Map[String, AnyRef]]()
    collection.find().into(allItems)

    allItems
  }

  override def getItem(
    tableName: String,
    key: MapStoreKey
  ): Optional[java.util.Map[String, AnyRef]] = {
    val collection = mongoDatabase.getCollection(tableName)

    val filter = createFilter(key)
    val result = collection.find(filter).first()

    Option(result).map(_.asInstanceOf[java.util.Map[String, AnyRef]]).asJava
  }

  override def getItems(
    tableName: String,
    key: MapStoreKey,
    options: MapQueryOptions
  ): java.util.List[java.util.Map[String, AnyRef]] = {
    val collection = mongoDatabase.getCollection(tableName)

    val findOptions = new FindOptions()
    key.getRangeField.asScala.foreach(rangeField => {
      options.getOrder match {
        case Order.ASC => findOptions.sort(Sorts.ascending(rangeField))
        case Order.DESC => findOptions.sort(Sorts.descending(rangeField))
      }
    })
    options.getLimit.asScala.foreach(limit => findOptions.limit(limit))

    val filter = createFilter(key)
    val items = new java.util.ArrayList[java.util.Map[String, AnyRef]]()
    collection.find(filter).limit(findOptions.getLimit).sort(findOptions.getSort).into(items)
    items
  }

  override def putItem(
    tableName: String,
    key: MapStoreKey,
    payload: java.util.Map[String, AnyRef]
  ): java.util.Map[String, AnyRef] = {
    val collection = mongoDatabase.getCollection(tableName)

    // Since we're treating MongoDB like a key-value store here,
    // we can just skip any document validation
    val insertOptions = new InsertOneOptions().bypassDocumentValidation(true)

    stripKey(key, payload)
    val document = new Document(payload)
    document.append(key.getHashField, key.getHashValue)
    key.getRangeField.asScala
      .foreach(rangeField => document.append(rangeField, key.getRangeKey.getValue.asInstanceOf[AnyRef]))

    collection.insertOne(document, insertOptions)
    document
  }

  override def updateItem(
    tableName: String,
    key: MapStoreKey,
    payload: java.util.Map[String, AnyRef]
  ): java.util.Map[String, AnyRef] = {
    val collection = mongoDatabase.getCollection(tableName)

    stripKey(key, payload)
    val document = new Document(payload)

    val filter = createFilter(key)
    val updateOptions = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
    collection.findOneAndUpdate(filter, document, updateOptions)
  }

  @VisibleForTesting
  private[mongodb] def createFilter(key: MapStoreKey): Bson = {
    val hashFilter = Filters.eq(key.getHashField, key.getHashValue)
    createRangeFilter(key).map(rangeFilter => Filters.and(hashFilter, rangeFilter))
      .getOrElse(hashFilter)
  }

  @VisibleForTesting
  private[mongodb] def createRangeFilter(key: MapStoreKey): Option[Bson] = {
    key.getRangeField.asScala.flatMap(rangeField => {
      (key.getRangeKey.getOperator, key.getRangeKey.getValue) match {
        case (Standard.ALL, _) => Option.empty[Bson]
        case (Standard.NONE, _) => Option.empty[Bson]
        case (Standard.BETWEEN, rValue) =>
          Option(
            Filters.and(
              Filters.gte(rangeField, rValue.asInstanceOf[TwoValueHolder[_]].getValue1),
              Filters.lte(rangeField, rValue.asInstanceOf[TwoValueHolder[_]].getValue2)
            )
          )
        case (Standard.EQUAL_TO, rValue) => Option(Filters.eq(rangeField, rValue))
        case (Standard.GREATER_THAN, rValue) => Option(Filters.gt(rangeField, rValue))
        case (Standard.GREATER_THAN_OR_EQUAL_TO, rValue) => Option(Filters.gte(rangeField, rValue))
        case (Standard.LESS_THAN, rValue) => Option(Filters.lt(rangeField, rValue))
        case (Standard.LESS_THAN_OR_EQUAL_TO, rValue) => Option(Filters.lte(rangeField, rValue))
        case (Extended.IN, rValue) => Option(Filters.in(rangeField, rValue.asInstanceOf[ListValueHolder[_]].getValues))
        case (operator, _) =>
          throw new IllegalArgumentException(s"MongoDB cannot support the operator ${operator}")
      }
    })
  }
}
