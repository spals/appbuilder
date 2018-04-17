package net.spals.appbuilder.mapstore.dynamodb

import java.util.Optional

import com.google.common.collect.ImmutableMap
import io.opentracing.mock.{MockSpan, MockTracer}
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions
import net.spals.appbuilder.mapstore.core.model.MapStoreIndexName.indexName
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.{equalTo => range_equalTo}
import net.spals.appbuilder.mapstore.core.model.{MapStoreIndexName, MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.dynamodb.DynamoDBSpanMatcher.dynamoDBSpan
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers._
import org.slf4j.LoggerFactory
import org.testng.annotations._

import scala.collection.JavaConverters._

/**
  * Integration tests for [[DynamoDBMapStoreIndexPlugin]].
  *
  * @author tkral
  */
class DynamoDBMapStoreIndexPluginIT {
  private val LOGGER = LoggerFactory.getLogger(classOf[DynamoDBMapStoreIndexPluginIT])

  private val dynamoDBEndpoint = s"http://${System.getenv("DYNAMODB_IP")}:${System.getenv("DYNAMODB_PORT")}"
  private val dynamoDBTracer = new MockTracer()
  private lazy val dynamoDBClient = {
    val dynamoDBClientProvider = new DynamoDBClientProvider(dynamoDBTracer)
    dynamoDBClientProvider.credentialsProviderClassName = "com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider"
    dynamoDBClientProvider.endpoint = dynamoDBEndpoint

    LOGGER.info(s"Connecting to dynamoDB instance at ${dynamoDBClientProvider.endpoint}")
    dynamoDBClientProvider.get()
  }

  private lazy val mapStorePlugin = {
    val plugin = new DynamoDBMapStorePlugin(dynamoDBClient)
    plugin.synchronousDDL = true
    plugin
  }
  private lazy val mapStoreIndexPlugin = {
    val plugin = new DynamoDBMapStoreIndexPlugin(dynamoDBClient)
    plugin.synchronousDDL = true
    plugin
  }

  private val hashIndexName = indexName("DynamoDBMapStoreIndexPluginIT_hashTable", "hashIndex")
  private val hashTableKey = new MapStoreTableKey.Builder()
    .setHash("tableHashField", classOf[String])
    .build
  private val hashIndexKey = new MapStoreTableKey.Builder()
    .setHash("indexHashField", classOf[String])
    .build

  private val rangeIndexName = indexName("DynamoDBMapStoreIndexPluginIT_rangeTable", "rangeIndex")
  private val rangeTableKey = new MapStoreTableKey.Builder()
    .setHash("tableHashField", classOf[String])
    .setRange("tableRangeField", classOf[String])
    .build
  private val rangeIndexKey = new MapStoreTableKey.Builder()
    .setHash("indexHashField", classOf[String])
    .setRange("indexRangeField", classOf[String])
    .build

  @BeforeClass def createTablesAndIndexes() {
    mapStorePlugin.createTable(hashIndexName.getTableName, hashTableKey)
    mapStoreIndexPlugin.createIndex(hashIndexName, hashIndexKey)

    mapStorePlugin.createTable(rangeIndexName.getTableName, rangeTableKey)
    mapStoreIndexPlugin.createIndex(rangeIndexName, rangeIndexKey)
  }

  @BeforeMethod def resetTracer() {
    dynamoDBTracer.reset()
  }

  @AfterClass(alwaysRun = true) def tearDownClass() {
    mapStoreIndexPlugin.dropIndex(hashIndexName)
    mapStoreIndexPlugin.dropIndex(rangeIndexName)

    mapStorePlugin.dropTable(hashIndexName.getTableName)
    mapStorePlugin.dropTable(rangeIndexName.getTableName)

    mapStoreIndexPlugin.close()
    mapStorePlugin.close()
  }

  @DataProvider def emptyGetProvider(): Array[Array[AnyRef]] = {
    Array(
      // Case: Hash-only key
      Array(hashIndexName,
        new MapStoreKey.Builder().setHash("indexHashField", "deadbeef").build),
      // Case: Hash and range key
      Array(rangeIndexName,
        new MapStoreKey.Builder().setHash("indexHashField", "deadbeef")
          .setRange("indexRangeField", range_equalTo[String]("deadbeef")).build)
    )
  }

  @Test(
    dataProvider = "emptyGetProvider",
    groups = Array("DynamoDBMapStoreIndexPluginIT.empty")
  )
  def testEmptyGetItem(
    indexName: MapStoreIndexName,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStoreIndexPlugin.getItem(indexName, storeKey), is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @Test(
    dataProvider = "emptyGetProvider",
    groups = Array("DynamoDBMapStoreIndexPluginIT.empty")
  )
  def testEmptyGetItems(
    indexName: MapStoreIndexName,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStoreIndexPlugin.getItems(indexName, storeKey, defaultOptions()), empty[java.util.Map[String, AnyRef]])
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @Test(
    groups = Array("DynamoDBMapStoreIndexPluginIT.get"),
    dependsOnGroups = Array("DynamoDBMapStoreIndexPluginIT.empty")
  )
  def testGetItemHashOnly() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build
    mapStorePlugin.putItem(hashIndexName.getTableName, tableItemKey,
      Map("indexHashField" -> "index").asJava.asInstanceOf[java.util.Map[String, AnyRef]])

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build
    val indexItem = mapStoreIndexPlugin.getItem(hashIndexName, indexItemKey)
    assertThat(indexItem, not(Optional.empty[java.util.Map[String, AnyRef]]))

    val itemMatcher: Matcher[java.util.Map[String, AnyRef]] =
      equalTo(ImmutableMap.of("tableHashField", "table", "indexHashField", "index"))
    assertThat(indexItem.get, itemMatcher)
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST")
    ))
  }

  @Test(
    groups = Array("DynamoDBMapStoreIndexPluginIT.get"),
    dependsOnGroups = Array("DynamoDBMapStoreIndexPluginIT.empty")
  )
  def testGetItemHashRange() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table")
      .setRange("tableRangeField", range_equalTo[String]("1")).build
    mapStorePlugin.putItem(rangeIndexName.getTableName, tableItemKey,
      Map("indexHashField" -> "index", "indexRangeField" -> "11").asJava.asInstanceOf[java.util.Map[String, AnyRef]])

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index")
      .setRange("indexRangeField", range_equalTo[String]("11")).build
    val indexItem = mapStoreIndexPlugin.getItem(rangeIndexName, indexItemKey)
    assertThat(indexItem, not(Optional.empty[java.util.Map[String, AnyRef]]))

    val itemMatcher: Matcher[java.util.Map[String, AnyRef]] =
      equalTo(ImmutableMap.of(
        "tableHashField", "table", "tableRangeField", "1",
        "indexHashField", "index", "indexRangeField", "11"
      ))
    assertThat(indexItem.get, itemMatcher)
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST")
    ))
  }

  @Test(
    groups = Array("DynamoDBMapStoreIndexPluginIT.update"),
    dependsOnGroups = Array("DynamoDBMapStoreIndexPluginIT.get")
  )
  def testUpdateItemHashOnly() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build
    mapStorePlugin.updateItem(hashIndexName.getTableName, tableItemKey,
      Map("key" -> "value").asJava.asInstanceOf[java.util.Map[String, AnyRef]])

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build
    val indexItem = mapStoreIndexPlugin.getItem(hashIndexName, indexItemKey)
    assertThat(indexItem, not(Optional.empty[java.util.Map[String, AnyRef]]))

    val itemMatcher: Matcher[java.util.Map[String, AnyRef]] =
      equalTo(ImmutableMap.of("tableHashField", "table", "indexHashField", "index", "key", "value"))
    assertThat(indexItem.get, itemMatcher)
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST")
    ))
  }

  @Test(
    groups = Array("DynamoDBMapStoreIndexPluginIT.update"),
    dependsOnGroups = Array("DynamoDBMapStoreIndexPluginIT.get")
  )
  def testUpdateItemHashRange() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table")
      .setRange("tableRangeField", range_equalTo[String]("1")).build
    mapStorePlugin.updateItem(rangeIndexName.getTableName, tableItemKey,
      Map("key" -> "value").asJava.asInstanceOf[java.util.Map[String, AnyRef]])

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index")
      .setRange("indexRangeField", range_equalTo[String]("11")).build
    val indexItem = mapStoreIndexPlugin.getItem(rangeIndexName, indexItemKey)
    assertThat(indexItem, not(Optional.empty[java.util.Map[String, AnyRef]]))

    val itemMatcher: Matcher[java.util.Map[String, AnyRef]] =
      equalTo(ImmutableMap.of(
        "tableHashField", "table", "tableRangeField", "1",
        "indexHashField", "index", "indexRangeField", "11",
        "key", "value"
      ))
    assertThat(indexItem.get, itemMatcher)
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST")
    ))
  }

  @Test(
    groups = Array("DynamoDBMapStoreIndexPluginIT.delete"),
    dependsOnGroups = Array("DynamoDBMapStoreIndexPluginIT.update")
  )
  def testDeleteItemHashOnly() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build
    mapStorePlugin.deleteItem(hashIndexName.getTableName, tableItemKey)

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build
    val indexItem = mapStoreIndexPlugin.getItem(hashIndexName, indexItemKey)
    assertThat(indexItem, is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST")
    ))
  }

  @Test(
    groups = Array("DynamoDBMapStoreIndexPluginIT.delete"),
    dependsOnGroups = Array("DynamoDBMapStoreIndexPluginIT.update")
  )
  def testDeleteItemHashRange() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table")
      .setRange("tableRangeField", range_equalTo[String]("1")).build
    mapStorePlugin.deleteItem(rangeIndexName.getTableName, tableItemKey)

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index")
      .setRange("indexRangeField", range_equalTo[String]("11")).build
    val indexItem = mapStoreIndexPlugin.getItem(rangeIndexName, indexItemKey)
    assertThat(indexItem, is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST")
    ))
  }
}
