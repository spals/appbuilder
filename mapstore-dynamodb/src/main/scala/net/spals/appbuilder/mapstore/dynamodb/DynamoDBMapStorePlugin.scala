package net.spals.appbuilder.mapstore.dynamodb

import java.util.Optional
import javax.annotation.PreDestroy
import javax.validation.constraints.NotNull

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils
import com.google.inject.Inject
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.mapstore.core.MapStorePlugin
import net.spals.appbuilder.mapstore.core.MapStorePlugin.stripKey
import net.spals.appbuilder.mapstore.core.model.{MapQueryOptions, MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.dynamodb.DynamoDBMapStoreUtil.{createAttributeType, createPrimaryKey, createQuerySpec}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
  * Implementation of [[MapStorePlugin]] which
  * uses AWS DynamoDB.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MapStorePlugin], key = "dynamoDB")
private[dynamodb] class DynamoDBMapStorePlugin @Inject() (
  dynamoDBClient: AmazonDynamoDB
) extends MapStorePlugin {
  private val LOGGER = LoggerFactory.getLogger(classOf[DynamoDBMapStorePlugin])

  @NotNull
  @Configuration("mapStore.dynamoDB.synchronousDDL")
  private[dynamodb] var synchronousDDL: Boolean = false

  private val dynamoDB = new DynamoDB(dynamoDBClient)

  @PreDestroy
  override def close() = dynamoDBClient.shutdown()

  override def createTable(
    tableName: String,
    tableKey: MapStoreTableKey
  ): Boolean = {
    // Add hash key information to the create table request
    val hashKeyAttrDef = new AttributeDefinition().withAttributeName(tableKey.getHashField)
      .withAttributeType(createAttributeType(tableKey.getHashFieldType))
    val hashKeySchema = new KeySchemaElement(tableKey.getHashField, KeyType.HASH)

    val createTableRequest = new CreateTableRequest().withTableName(tableName)
      .withAttributeDefinitions(hashKeyAttrDef)
      .withKeySchema(hashKeySchema)

    // Add range key information to the create table request (if necessary)
    tableKey.getRangeField.asScala.foreach(rangeField => {
      val rangeKeyAttrDef = new AttributeDefinition().withAttributeName(rangeField)
        .withAttributeType(createAttributeType(tableKey.getRangeFieldType.get()))
      val rangeKeySchema = new KeySchemaElement(rangeField, KeyType.RANGE)

      createTableRequest.withAttributeDefinitions(rangeKeyAttrDef)
        .withKeySchema(rangeKeySchema)
    })

    // Add minimal provisioned throughput to the table
    val provisionedThroughput = new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L)
    createTableRequest.withProvisionedThroughput(provisionedThroughput)

    try {
      TableUtils.createTableIfNotExists(dynamoDBClient, createTableRequest)
      // If we fall through the create-if-not-exists without error
      // then the table exists
      synchronousDDL match {
        case false => true
        case true => {
          val tableDescription = dynamoDB.getTable(tableName).waitForActive()
          TableStatus.ACTIVE.toString.equals(tableDescription.getTableStatus)
        }
      }
    } catch {
      case _: RuntimeException => false
    }
  }

  override def dropTable(tableName: String): Boolean = {
    val table = dynamoDB.getTable(tableName)

    val deleteTableRequest = new DeleteTableRequest().withTableName(tableName)
    try {
      TableUtils.deleteTableIfExists(dynamoDBClient, deleteTableRequest)
      // if we fall through the delete-if-exists without error
      // then the table not longer exists
      synchronousDDL match {
        case false => true
        case true => {
          table.waitForDelete()
          true
        }
      }
    } catch {
      case _: RuntimeException => false
    }
  }

  override def deleteItem(
    tableName: String,
    key: MapStoreKey
  ): Unit = {
    val table = dynamoDB.getTable(tableName)
    val primaryKey = createPrimaryKey(key)

    val deleteItemOutcome = table.deleteItem(primaryKey)
    if (LOGGER.isTraceEnabled) {
      LOGGER.trace(s"Capacity used for deleteItem on table $tableName: " +
        s"${deleteItemOutcome.getDeleteItemResult.getConsumedCapacity}")
    }
  }

  override def getAllItems(tableName: String): java.util.List[java.util.Map[String, AnyRef]] = {
    val table = dynamoDB.getTable(tableName)
    table.scan(new ScanSpec).asScala.map(_.asMap()).toList.asJava
  }

  override def getItem(
    tableName: String,
    key: MapStoreKey
  ): Optional[java.util.Map[String, AnyRef]] = {
    val table = dynamoDB.getTable(tableName)
    val primaryKey = createPrimaryKey(key)

    val getItemOutcome = table.getItemOutcome(primaryKey)
    if (LOGGER.isTraceEnabled) {
      LOGGER.trace(s"Capacity used for getItem on table $tableName: " +
        s"${getItemOutcome.getGetItemResult.getConsumedCapacity}")
    }

    Option(getItemOutcome.getItem).map(_.asMap()).asJava
  }

  override def getItems(
    tableName: String,
    key: MapStoreKey,
    options: MapQueryOptions
  ): java.util.List[java.util.Map[String, AnyRef]] = {
    val table = dynamoDB.getTable(tableName)
    val querySpec = createQuerySpec(key, options)

    table.query(querySpec).asScala.map(_.asMap()).toList.asJava
  }

  override def putItem(
    tableName: String,
    key: MapStoreKey,
    payload: java.util.Map[String, AnyRef]
  ): java.util.Map[String, AnyRef] = {
    val table = dynamoDB.getTable(tableName)
    val primaryKey = createPrimaryKey(key)

    stripKey(key, payload)
    val item = Item.fromMap(payload).withPrimaryKey(primaryKey)
    val putItemOutcome = table.putItem(item)
    if (LOGGER.isTraceEnabled) {
      LOGGER.trace(s"Capacity used for putItem on table $tableName: " +
        s"${putItemOutcome.getPutItemResult.getConsumedCapacity}")
    }

    Option(putItemOutcome.getItem).map(_.asMap()).getOrElse(item.asMap())
  }

  override def updateItem(
    tableName: String,
    key: MapStoreKey,
    payload: java.util.Map[String, AnyRef]
  ): java.util.Map[String, AnyRef] = {
    val table = dynamoDB.getTable(tableName)
    val primaryKey = createPrimaryKey(key)

    stripKey(key, payload)
    val attrUpdates = payload.entrySet().asScala
      .map(entry => {
        val attrUpdate = new AttributeUpdate(entry.getKey)
        entry.getValue match {
          case null | "" => attrUpdate.delete()
          case n: java.lang.Number => attrUpdate.addNumeric(n)
          case value => attrUpdate.put(value)
        }
      }).toArray

    val updateItemOutcome = table.updateItem(primaryKey, attrUpdates: _*)
    if (LOGGER.isTraceEnabled) {
      LOGGER.trace(s"Capacity used for updateItem on table $tableName: " +
        s"${updateItemOutcome.getUpdateItemResult.getConsumedCapacity}")
    }

    Option(updateItemOutcome.getItem).map(_.asMap())
      .getOrElse(table.getItem(primaryKey).asMap())
  }
}
