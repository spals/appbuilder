package net.spals.appbuilder.mapstore.dynamodb

import java.util.Optional

import io.opentracing.mock.{MockSpan, MockTracer}
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.{equalTo => range_equalTo, greaterThan => range_greaterThan, greaterThanOrEqualTo => range_greaterThanOrEqualTo, lessThan => range_lessThan, lessThanOrEqualTo => range_lessThanOrEqualTo, startsWith => range_startsWith}
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.between
import net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.all
import net.spals.appbuilder.mapstore.core.model.{MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.dynamodb.DynamoDBSpanMatcher.dynamoDBSpan
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers._
import org.slf4j.LoggerFactory
import org.testng.annotations._

import scala.collection.JavaConverters._

/**
  * Integration tests for [[DynamoDBMapStorePlugin]].
  *
  * @author tkral
  */
class DynamoDBMapStorePluginIT {
  private val LOGGER = LoggerFactory.getLogger(classOf[DynamoDBMapStorePluginIT])

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

  private val hashTableName = "DynamoDBMapStorePluginIT_hashTable"
  private val hashTableKey = new MapStoreTableKey.Builder()
    .setHash("myHashField", classOf[String])
    .build

  private val rangeTableName = "DynamoDBMapStorePluginIT_rangeTable"
  private val rangeTableKey = new MapStoreTableKey.Builder()
    .setHash("myHashField", classOf[String])
    .setRange("myRangeField", classOf[String])
    .build()

  @BeforeClass def createTables() {
    mapStorePlugin.createTable(hashTableName, hashTableKey)
    mapStorePlugin.createTable(rangeTableName, rangeTableKey)
  }

  @BeforeMethod def resetTracer() {
    dynamoDBTracer.reset()
  }

  @AfterClass(alwaysRun = true) def tearDownClass() {
    mapStorePlugin.dropTable(hashTableName)
    mapStorePlugin.dropTable(rangeTableName)

    mapStorePlugin.close()
  }

  @Test def testCreateTableIdempotent() {
    assertThat(mapStorePlugin.createTable(hashTableName, hashTableKey), is(true))
  }

  @DataProvider def emptyGetProvider(): Array[Array[AnyRef]] = {
    Array(
      // Case: Hash-only key
      Array(hashTableName,
        new MapStoreKey.Builder().setHash("myHashField", "deadbeef").build),
      // Case: Hash and range key
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "deadbeef")
          .setRange("myRangeField", range_equalTo[String]("deadbeef")).build)
    )
  }

  @Test(
    dataProvider = "emptyGetProvider",
    groups = Array("DynamoDBMapStorePluginIT.empty")
  )
  def testEmptyGetItem(
    tableName: String,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStorePlugin.getItem(tableName, storeKey), is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @Test(
    dataProvider = "emptyGetProvider",
    groups = Array("DynamoDBMapStorePluginIT.empty")
  )
  def testEmptyGetItems(
    tableName: String,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStorePlugin.getItems(tableName, storeKey, defaultOptions()), empty[java.util.Map[String, AnyRef]])
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @DataProvider def putItemProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(hashTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue").build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "key" -> "value")),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue1")).build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue1", "key" -> "value")),
      // Inserted for getItems tests below
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue2")).build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue2", "key" -> "value")),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue3")).build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue3", "key" -> "value")),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue4")).build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue4", "key" -> "value"))
    )
  }

  @Test(
    dataProvider = "putItemProvider",
    groups = Array("DynamoDBMapStorePluginIT.put"),
    dependsOnGroups = Array("DynamoDBMapStorePluginIT.empty")
  )
  def testPutItem(
    tableName: String,
    storeKey: MapStoreKey,
    payload: Map[String, AnyRef],
    expectedResult: Map[String, AnyRef]
  ) {
    assertThat(mapStorePlugin.putItem(tableName, storeKey, payload.asJava), is(expectedResult.asJava))
    assertThat(mapStorePlugin.getItem(tableName, storeKey), is(Optional.of(expectedResult.asJava)))
    assertThat(dynamoDBTracer.finishedSpans(),
      contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST"), dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @DataProvider def updateItemProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(Map("numberKey" -> Long.box(1L)),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue1", "key" -> "value", "numberKey" -> java.math.BigDecimal.valueOf(1L))),
      Array(Map("numberKey" -> ""),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue1", "key" -> "value"))
    )
  }

  @Test(
    dataProvider = "updateItemProvider",
    groups = Array("DynamoDBMapStorePluginIT.update"),
    dependsOnGroups = Array("DynamoDBMapStorePluginIT.put")
  )
  def testUpdateItem(
    payload: Map[String, AnyRef],
    expectedResult: Map[String, AnyRef]
  ) {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", range_equalTo[String]("myRangeValue1")).build

    assertThat(mapStorePlugin.updateItem(rangeTableName, storeKey, payload.asJava), is(expectedResult.asJava))
    assertThat(mapStorePlugin.getItem(rangeTableName, storeKey), is(Optional.of(expectedResult.asJava)))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST"), dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @Test(
    groups = Array("DynamoDBMapStorePluginIT.get"),
    dependsOnGroups = Array("DynamoDBMapStorePluginIT.put", "DynamoDBMapStorePluginIT.update")
  )
  def testGetAllItems() {
    assertThat(mapStorePlugin.getAllItems(rangeTableName),
      contains[java.util.Map[String, AnyRef]](result(1).asJava, result(2).asJava, result(3).asJava, result(4).asJava))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @DataProvider def getItemsProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", all()).build,
        List(result(1), result(2), result(3), result(4))),
      // Case: Between different values
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", between[String]("myRangeValue2", "myRangeValue4")).build,
        List(result(2), result(3), result(4))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", between[String]("myRangeValue2", "myRangeValue2")).build,
        List(result(2))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_equalTo[String]("myRangeValue1")).build,
        List(result(1))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_greaterThan[String]("myRangeValue2")).build,
        List(result(3), result(4))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_greaterThanOrEqualTo[String]("myRangeValue2")).build,
        List(result(2), result(3), result(4))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_lessThan[String]("myRangeValue3")).build,
        List(result(1), result(2))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_lessThanOrEqualTo[String]("myRangeValue3")).build,
        List(result(1), result(2), result(3))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_startsWith("myRangeValue")).build,
        List(result(1), result(2), result(3), result(4)))
    )
  }

  @Test(
    dataProvider = "getItemsProvider",
    groups = Array("DynamoDBMapStorePluginIT.get"),
    dependsOnGroups = Array("DynamoDBMapStorePluginIT.put", "DynamoDBMapStorePluginIT.update")
  )
  def testGetItems(
    storeKey: MapStoreKey,
    expectedResults: List[Map[String, AnyRef]]
  ) {
    assertThat(mapStorePlugin.getItems(rangeTableName, storeKey, defaultOptions()),
      contains[java.util.Map[String, AnyRef]](expectedResults.map(_.asJava): _*))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @Test(
    groups = Array("DynamoDBMapStorePluginIT.delete"),
    dependsOnGroups = Array("DynamoDBMapStorePluginIT.get")
  )
  def testDeleteItem() {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", range_equalTo[String]("myRangeValue4")).build
    mapStorePlugin.deleteItem(rangeTableName, storeKey)

    assertThat(mapStorePlugin.getAllItems(rangeTableName),
      contains[java.util.Map[String, AnyRef]](result(1).asJava, result(2).asJava, result(3).asJava))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](
      dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST"))
    )
  }

  def result(i: Int): Map[String, AnyRef] = {
    Map("myHashField" -> "myHashValue", "myRangeField" -> s"myRangeValue$i", "key" -> "value")
  }
}
