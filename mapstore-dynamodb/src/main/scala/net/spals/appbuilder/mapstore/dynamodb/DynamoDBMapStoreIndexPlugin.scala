package net.spals.appbuilder.mapstore.dynamodb

import java.util
import java.util.Optional
import javax.annotation.PreDestroy
import javax.validation.constraints.NotNull

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.google.inject.Inject
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.mapstore.core.MapStoreIndexPlugin
import net.spals.appbuilder.mapstore.core.model.{MapQueryOptions, MapStoreIndexName, MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.dynamodb.DynamoDBMapStoreUtil.{createAttributeType, createQuerySpec}

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
  * Implementation of [[MapStoreIndexPlugin]] which
  * uses AWS DynamoDB.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MapStoreIndexPlugin], key = "dynamoDB")
private[dynamodb] class DynamoDBMapStoreIndexPlugin @Inject() (
  dynamoDBClient: AmazonDynamoDB
) extends MapStoreIndexPlugin {

  @NotNull
  @Configuration("mapStore.dynamoDB.synchronousDDL")
  private[dynamodb] var synchronousDDL: Boolean = false

  private val dynamoDB = new DynamoDB(dynamoDBClient)

  @PreDestroy
  override def close() = dynamoDB.shutdown()

  override def createIndex(
    indexName: MapStoreIndexName,
    indexKey: MapStoreTableKey
  ): Boolean = {
    // Add hash key information to the create index request
    val hashKeyAttrDef = new AttributeDefinition().withAttributeName(indexKey.getHashField)
      .withAttributeType(createAttributeType(indexKey.getHashFieldType))
    val hashKeySchema: KeySchemaElement = new KeySchemaElement(indexKey.getHashField, KeyType.HASH)

    val createGSIAction = new CreateGlobalSecondaryIndexAction()
      .withIndexName(indexName.getIndexName)
      .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
      .withKeySchema(hashKeySchema)

    val rangeKeyAttrDefOpt = indexKey.getRangeField.asScala.map(rangeField =>
      new AttributeDefinition().withAttributeName(rangeField)
        .withAttributeType(createAttributeType(indexKey.getRangeFieldType.get())))
    rangeKeyAttrDefOpt.foreach(rangeKey => {
      val rangeKeySchema = new KeySchemaElement(rangeKey.getAttributeName, KeyType.RANGE)
      createGSIAction.withKeySchema(rangeKeySchema)
    })

    // Add minimal provisioned throughput to the index
    val provisionedThroughput = new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L)
    createGSIAction.withProvisionedThroughput(provisionedThroughput)

    val table = dynamoDB.getTable(indexName.getTableName)
    val index = rangeKeyAttrDefOpt match {
      case Some(rangeKeyAttrDef) => table.createGSI(createGSIAction, hashKeyAttrDef, rangeKeyAttrDef)
      case None => table.createGSI(createGSIAction, hashKeyAttrDef)
    }

    synchronousDDL match {
      case false => true
      case true => {
        val indexDescription = index.waitForActive()
        TableStatus.ACTIVE.toString.equals(indexDescription.getTableStatus)
      }
    }
  }

  override def dropIndex(indexName: MapStoreIndexName): Boolean = {
    val table = dynamoDB.getTable(indexName.getTableName)
    val index = table.getIndex(indexName.getIndexName)
    index.deleteGSI()

    synchronousDDL match {
      case false => true
      case true => {
        val indexDescription = index.waitForDelete()
        TableStatus.ACTIVE.toString.equals(indexDescription.getTableStatus)
      }
    }
    true
  }

  override def getItem(
    indexName: MapStoreIndexName,
    key: MapStoreKey
  ): Optional[util.Map[String, AnyRef]] = {
    val table = dynamoDB.getTable(indexName.getTableName)
    val index = table.getIndex(indexName.getIndexName)

    val singleOptions = new MapQueryOptions.Builder().setLimit(1).build()
    val querySpec = createQuerySpec(key, singleOptions)

    Option(index.query(querySpec).asScala.map(_.asMap()).toList)
      .filter(!_.isEmpty)
      .map(_.head)
      .asJava
  }

  override def getItems(
    indexName: MapStoreIndexName,
    key: MapStoreKey,
    options: MapQueryOptions
  ): util.List[util.Map[String, AnyRef]] = {
    val table = dynamoDB.getTable(indexName.getTableName)
    val index = table.getIndex(indexName.getIndexName)
    val querySpec = createQuerySpec(key, options)

    index.query(querySpec).asScala.map(_.asMap()).toList.asJava
  }
}
