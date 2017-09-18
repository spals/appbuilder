package net.spals.appbuilder.mapstore.dynamodb

import java.util.Optional

import io.opentracing.mock.{MockSpan, MockTracer}
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey._
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.between
import net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.all
import net.spals.appbuilder.mapstore.core.model.{MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.dynamodb.DynamoDBSpanMatcher.dynamoDBSpan
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{contains, empty, is}
import org.hamcrest.{Description, Matchers, TypeSafeMatcher}
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
    dynamoDBClientProvider.awsAccessKeyId = "DUMMY"
    dynamoDBClientProvider.awsSecretKey = "DUMMY"
    dynamoDBClientProvider.endpoint = dynamoDBEndpoint

    LOGGER.info(s"Connecting to dynamoDB instance at ${dynamoDBClientProvider.endpoint}")
    dynamoDBClientProvider.get()
  }

  private lazy val mapStorePlugin = new DynamoDBMapStorePlugin(dynamoDBClient)

  private val hashTableName = "hashTable"
  private val hashTableKey = new MapStoreTableKey.Builder()
    .setHash("myHashField", classOf[String])
    .build

  private val rangeTableName = "rangeTable"
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

  @AfterClass(alwaysRun = true) def dropTables() {
    mapStorePlugin.dropTable(hashTableName)
    mapStorePlugin.dropTable(rangeTableName)
  }

  @DataProvider def emptyGetProvider(): Array[Array[AnyRef]] = {
    Array(
      // Case: Hash-only key
      Array(hashTableName,
        new MapStoreKey.Builder().setHash("myHashField", "deadbeef").build),
      // Case: Hash and range key
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "deadbeef")
          .setRange("myRangeField", equalTo[String]("deadbeef")).build)
    )
  }

  @Test(dataProvider = "emptyGetProvider")
  def testEmptyGetItem(tableName: String,
                       storeKey: MapStoreKey) {
    assertThat(mapStorePlugin.getItem(tableName, storeKey), is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @Test(dataProvider = "emptyGetProvider")
  def testEmptyGetItems(tableName: String,
                        storeKey: MapStoreKey) {
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
          .setRange("myRangeField", equalTo[String]("myRangeValue1")).build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue1", "key" -> "value")),
      // Inserted for getItems tests below
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", equalTo[String]("myRangeValue2")).build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue2", "key" -> "value")),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", equalTo[String]("myRangeValue3")).build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue3", "key" -> "value")),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", equalTo[String]("myRangeValue4")).build,
        Map("key" -> "value"),
        Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue4", "key" -> "value"))
    )
  }

  @Test(dataProvider = "putItemProvider")
  def testPutItem(tableName: String,
                  storeKey: MapStoreKey,
                  payload: Map[String, AnyRef],
                  expectedResult: Map[String, AnyRef]) {
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

  @Test(dataProvider = "updateItemProvider", dependsOnMethods = Array("testPutItem"))
  def testUpdateItem(payload: Map[String, AnyRef],
                     expectedResult: Map[String, AnyRef]) {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", equalTo[String]("myRangeValue1")).build

    assertThat(mapStorePlugin.updateItem(rangeTableName, storeKey, payload.asJava), is(expectedResult.asJava))
    assertThat(mapStorePlugin.getItem(rangeTableName, storeKey), is(Optional.of(expectedResult.asJava)))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST"),
      dynamoDBSpan(dynamoDBEndpoint, "POST"), dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }

  @DataProvider def getItemsProvider(): Array[Array[AnyRef]] = {
    val result: Int => Map[String, AnyRef] = i => Map("myHashField" -> "myHashValue",
      "myRangeField" -> s"myRangeValue$i", "key" -> "value")

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
        .setRange("myRangeField", equalTo[String]("myRangeValue1")).build,
        List(result(1))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", greaterThan[String]("myRangeValue2")).build,
        List(result(3), result(4))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", greaterThanOrEqualTo[String]("myRangeValue2")).build,
        List(result(2), result(3), result(4))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", lessThan[String]("myRangeValue3")).build,
        List(result(1), result(2))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", lessThanOrEqualTo[String]("myRangeValue3")).build,
        List(result(1), result(2), result(3))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", startsWith("myRangeValue")).build,
        List(result(1), result(2), result(3), result(4)))
    )
  }

  @Test(dataProvider = "getItemsProvider", dependsOnMethods = Array("testPutItem", "testUpdateItem"))
  def testGetItems(storeKey: MapStoreKey,
                   expectedResults: List[Map[String, AnyRef]]) {
    assertThat(mapStorePlugin.getItems(rangeTableName, storeKey, defaultOptions()),
      contains[java.util.Map[String, AnyRef]](expectedResults.map(_.asJava): _*))
    assertThat(dynamoDBTracer.finishedSpans(), contains[MockSpan](dynamoDBSpan(dynamoDBEndpoint, "POST")))
  }
}

private object DynamoDBSpanMatcher {

  def dynamoDBSpan(url: String, method: String): DynamoDBSpanMatcher =
    DynamoDBSpanMatcher(url, method)
}

private case class DynamoDBSpanMatcher(url: String, method: String) extends TypeSafeMatcher[MockSpan] {

  override def matchesSafely(mockSpan: MockSpan): Boolean = {
    Matchers.hasEntry[String, AnyRef]("component", "java-aws-sdk").matches(mockSpan.tags()) &&
      Matchers.hasEntry[String, AnyRef]("http.method", method).matches(mockSpan.tags()) &&
      Matchers.hasEntry[String, AnyRef]("http.url", url).matches(mockSpan.tags()) &&
      Matchers.hasEntry[String, AnyRef]("span.kind", "client").matches(mockSpan.tags()) &&
      "AmazonDynamoDBv2".equals(mockSpan.operationName())
  }

  override def describeTo(description: Description): Unit = {
    description.appendText("a DynamoDB span tagged with method ")
    description.appendText(method)
    description.appendText(" and url ")
    description.appendText(url)
  }
}