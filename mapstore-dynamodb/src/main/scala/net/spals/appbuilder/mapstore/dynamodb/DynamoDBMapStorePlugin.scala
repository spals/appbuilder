package net.spals.appbuilder.mapstore.dynamodb

import java.io.Closeable
import java.util.Optional
import javax.annotation.PreDestroy

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.document.spec.{QuerySpec, ScanSpec}
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils
import com.google.common.annotations.VisibleForTesting
import com.google.inject.Inject
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.mapstore.core.MapStorePlugin
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.{Extended, Standard}
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.TwoValueHolder
import net.spals.appbuilder.mapstore.core.model.{MapQueryOptions, MapStoreKey, MapStoreTableKey}

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
  * Implementation of [[MapStorePlugin]] which
  * uses AWS DynamoDB.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MapStorePlugin], key = "dynamoDB")
private[dynamodb] class DynamoDBMapStorePlugin @Inject() (dynamoDBClient: AmazonDynamoDB)
  extends MapStorePlugin with Closeable {

  private val dynamoDB = new DynamoDB(dynamoDBClient)

  @PreDestroy
  override def close() = dynamoDB.shutdown()

  override def createTable(tableName: String,
                           tableKey: MapStoreTableKey): Boolean = {
    val hashKeyAttrDef = new AttributeDefinition().withAttributeName(tableKey.getHashField)
      .withAttributeType(createAttributeType(tableKey.getHashFieldType))
    val hashKeySchema = new KeySchemaElement(tableKey.getHashField, KeyType.HASH)

    val createTableRequest = new CreateTableRequest().withTableName(tableName)
      .withAttributeDefinitions(hashKeyAttrDef)
      .withKeySchema(hashKeySchema)

    tableKey.getRangeField.asScala.foreach(rangeField => {
      val rangeKeyAttrDef = new AttributeDefinition().withAttributeName(rangeField)
        .withAttributeType(createAttributeType(tableKey.getRangeFieldType.get()))
      val rangeKeySchema = new KeySchemaElement(rangeField, KeyType.RANGE)

      createTableRequest.withAttributeDefinitions(rangeKeyAttrDef)
        .withKeySchema(rangeKeySchema)
    })

    TableUtils.createTableIfNotExists(dynamoDBClient, createTableRequest)
  }

  override def dropTable(tableName: String): Boolean = {
    val deleteTableRequest = new DeleteTableRequest().withTableName(tableName)
    TableUtils.deleteTableIfExists(dynamoDBClient, deleteTableRequest)
  }

  override def deleteItem(tableName: String,
                          key: MapStoreKey): Unit = {
    val table = dynamoDB.getTable(tableName)
    val primaryKey = createPrimaryKey(key)

    table.deleteItem(primaryKey)
  }

  override def getAllItems(tableName: String): java.util.List[java.util.Map[String, AnyRef]] = {
    val table = dynamoDB.getTable(tableName)
    table.scan(new ScanSpec).asScala.map(_.asMap()).toList.asJava
  }

  override def getItem(tableName: String,
                       key: MapStoreKey): Optional[java.util.Map[String, AnyRef]] = {
    val table = dynamoDB.getTable(tableName)
    val primaryKey = createPrimaryKey(key)

    Option(table.getItem(primaryKey)).map(_.asMap()).asJava
  }

  override def getItems(tableName: String,
                        key: MapStoreKey,
                        options: MapQueryOptions): java.util.List[java.util.Map[String, AnyRef]] = {
    val table = dynamoDB.getTable(tableName)
    val querySpec = new QuerySpec().withHashKey(key.getHashField, key.getHashValue)
    createRangeKeyCondition(key).foreach(rangeKeyCondition => querySpec.withRangeKeyCondition(rangeKeyCondition))

    querySpec.withScanIndexForward(options.getOrder == MapQueryOptions.Order.ASC)
    options.getLimit.asScala.foreach(limit => querySpec.withMaxResultSize(limit))

    table.query(querySpec).asScala.map(_.asMap()).toList.asJava
  }

  override def putItem(tableName: String,
                       key: MapStoreKey,
                       payload: java.util.Map[String, AnyRef]): java.util.Map[String, AnyRef] = {
    val table = dynamoDB.getTable(tableName)
    val primaryKey = createPrimaryKey(key)

    stripKey(key, payload)
    val item = Item.fromMap(payload).withPrimaryKey(primaryKey)
    table.putItem(item).getItem.asMap()
  }

  override def updateItem(tableName: String,
                          key: MapStoreKey,
                          payload: java.util.Map[String, AnyRef]): java.util.Map[String, AnyRef] = {
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

    table.updateItem(primaryKey, attrUpdates: _*).getItem.asMap()
  }

  @VisibleForTesting
  private[dynamodb] def createAttributeType(fieldType: Class[_]): ScalarAttributeType = {
    fieldType match {
      case fType if fType.equals(classOf[Boolean]) || fType.equals(classOf[java.lang.Boolean]) => ScalarAttributeType.B
      case fType if classOf[java.lang.Number].isAssignableFrom(fType) => ScalarAttributeType.N
      case _ => ScalarAttributeType.S
    }
  }

  @VisibleForTesting
  private[dynamodb] def createPrimaryKey(key: MapStoreKey): PrimaryKey = {
    key.getRangeField.asScala
      .map(rangeField => new PrimaryKey(key.getHashField, key.getHashValue, rangeField, key.getRangeKey.getValue))
      .getOrElse(new PrimaryKey(key.getHashField, key.getHashValue))
  }

  @VisibleForTesting
  private[dynamodb] def createRangeKeyCondition(key: MapStoreKey): Option[RangeKeyCondition] = {
    key.getRangeField.asScala.flatMap(rangeField => {
      val rangeKeyCondition = new RangeKeyCondition(rangeField)
      (key.getRangeKey.getOperator, key.getRangeKey.getValue) match {
        case (Standard.ALL, _) => Option.empty[RangeKeyCondition]
        case (Standard.NONE, _) => Option.empty[RangeKeyCondition]
        case (Standard.BETWEEN, rValue) =>
          Option(rangeKeyCondition.between(rValue.asInstanceOf[TwoValueHolder[_]].getValue1,
                 rValue.asInstanceOf[TwoValueHolder[_]].getValue2))
        case (Standard.EQUAL_TO, rValue) => Option(rangeKeyCondition.eq(rValue))
        case (Standard.GREATER_THAN, rValue) => Option(rangeKeyCondition.gt(rValue))
        case (Standard.GREATER_THAN_OR_EQUAL_TO, rValue) => Option(rangeKeyCondition.ge(rValue))
        case (Standard.LESS_THAN, rValue) => Option(rangeKeyCondition.lt(rValue))
        case (Standard.LESS_THAN_OR_EQUAL_TO, rValue) => Option(rangeKeyCondition.le(rValue))
        case (Extended.STARTS_WITH, rValue) => Option(rangeKeyCondition.beginsWith(rValue.asInstanceOf[String]))
        case (operator, _) =>
          throw new IllegalArgumentException(s"DynamoDB cannot support the operator $operator")
      }
    })
  }
}
