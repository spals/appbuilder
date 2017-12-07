package net.spals.appbuilder.mapstore.mongodb

import java.util.Optional

import io.opentracing.mock.{MockSpan, MockTracer}
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions
import net.spals.appbuilder.mapstore.core.model.MultiValueMapRangeKey.in
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey._
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.between
import net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.all
import net.spals.appbuilder.mapstore.core.model.{MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.mongodb.MongoDBSpanMatcher.mongoDBSpan
import org.bson.Document
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{contains, containsString, empty, hasEntry, is}
import org.hamcrest.{Description, Matcher, TypeSafeMatcher}
import org.slf4j.LoggerFactory
import org.testng.annotations._

import scala.collection.JavaConverters._

/**
  * Integration tests for [[MongoDBMapStorePlugin]].
  *
  * @author tkral
  */
class MongoDBMapStorePluginIT {
  private val LOGGER = LoggerFactory.getLogger(classOf[MongoDBMapStorePluginIT])

  private val mongoDBEndpoint = s"http://${System.getenv("MONGODB_IP")}:${System.getenv("MONGODB_PORT")}"
  private val mongoDBTracer = new MockTracer()
  private lazy val mongoClient = {
    val mongoClientProvider = new MongoClientProvider(mongoDBTracer)
    mongoClientProvider.host = System.getenv("MONGODB_IP")
    mongoClientProvider.port = System.getenv("MONGODB_PORT").asInstanceOf[Int]

    LOGGER.info(s"Connecting to mongoDB instance at ${mongoClientProvider.host}:${mongoClientProvider.port}")
    mongoClientProvider.get()
  }

  private val applicationName = "MongoDBMapStorePluginIT"
  private lazy val mongoDatabase = {
    val mongoDatabaseProvider = new MongoDatabaseProvider(applicationName, mongoClient)
    mongoDatabaseProvider.get()
  }
  private lazy val mapStorePlugin = new MongoDBMapStorePlugin(mongoClient, mongoDatabase)

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
    mongoDBTracer.reset()
  }

  @AfterClass(alwaysRun = true) def dropTables() {
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
          .setRange("myRangeField", equalTo[String]("deadbeef")).build)
    )
  }

  @Test(dataProvider = "emptyGetProvider")
  def testEmptyGetItem(
    tableName: String,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStorePlugin.getItem(tableName, storeKey), is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("find")))
  }

  @Test(dataProvider = "emptyGetProvider")
  def testEmptyGetItems(
    tableName: String,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStorePlugin.getItems(tableName, storeKey, defaultOptions()), empty[java.util.Map[String, AnyRef]])
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("find")))
  }

  @DataProvider def putItemProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(hashTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue").build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "key" -> "value").toMap[String, AnyRef].asJava)),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", equalTo[String]("myRangeValue1")).build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue1", "key" -> "value").toMap[String, AnyRef].asJava)),
      // Inserted for getItems tests below
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", equalTo[String]("myRangeValue2")).build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue2", "key" -> "value").toMap[String, AnyRef].asJava)),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", equalTo[String]("myRangeValue3")).build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue3", "key" -> "value").toMap[String, AnyRef].asJava)),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", equalTo[String]("myRangeValue4")).build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue4", "key" -> "value").toMap[String, AnyRef].asJava))
    )
  }

  @Test(dataProvider = "putItemProvider")
  def testPutItem(
    tableName: String,
    storeKey: MapStoreKey,
    payload: Map[String, AnyRef],
    expectedResult: Document
  ) {
    // The asserts here are technically backwards, but we have to
    assertThat(mapStorePlugin.putItem(tableName, storeKey, payload.asJava).asInstanceOf[Document],
      is(expectedResult))
    assertThat(mapStorePlugin.getItem(tableName, storeKey).asInstanceOf[Optional[Document]],
      is(Optional.of(expectedResult)))
    assertThat(mongoDBTracer.finishedSpans(),
      contains[MockSpan](mongoDBSpan("insert"), mongoDBSpan("find")))
  }

  @DataProvider def updateItemProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(Map("numberKey" -> Long.box(1L)),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue1",
          "key" -> "value", "numberKey" -> java.lang.Long.valueOf(1L)).toMap[String, AnyRef].asJava)),
      Array(Map("numberKey" -> ""),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue1",
          "key" -> "value").toMap[String, AnyRef].asJava))
    )
  }

  @Test(dataProvider = "updateItemProvider", dependsOnMethods = Array("testPutItem"))
  def testUpdateItem(
    payload: Map[String, AnyRef],
    expectedResult: Document
  ) {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", equalTo[String]("myRangeValue1")).build

    assertThat(mapStorePlugin.updateItem(rangeTableName, storeKey, payload.asJava).asInstanceOf[Document],
      is(expectedResult))
    assertThat(mapStorePlugin.getItem(rangeTableName, storeKey).asInstanceOf[Optional[Document]],
      is(Optional.of(expectedResult)))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("findandmodify"),
      mongoDBSpan("find")))
  }

  @DataProvider def getItemsProvider(): Array[Array[AnyRef]] = {
    val result: Int => Document = i => new Document(Map("myHashField" -> "myHashValue",
      "myRangeField" -> s"myRangeValue$i", "key" -> "value").toMap[String, AnyRef].asJava)

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
        .setRange("myRangeField", in[String]("myRangeValue2", "myRangeValue3")).build,
        List(result(2), result(3))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", lessThan[String]("myRangeValue3")).build,
        List(result(1), result(2))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", lessThanOrEqualTo[String]("myRangeValue3")).build,
        List(result(1), result(2), result(3)))
    )
  }

  @Test(dataProvider = "getItemsProvider", dependsOnMethods = Array("testPutItem", "testUpdateItem"))
  def testGetItems(
    storeKey: MapStoreKey,
    expectedResults: List[Document]
  ) {
    assertThat(mapStorePlugin.getItems(rangeTableName, storeKey, defaultOptions()).asInstanceOf[java.util.List[Document]],
      contains[Document](expectedResults: _*))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("find")))
  }
}

private object MongoDBSpanMatcher {

  def mongoDBSpan(operation: String): MongoDBSpanMatcher =
    MongoDBSpanMatcher(operation)
}

private case class MongoDBSpanMatcher(operation: String) extends TypeSafeMatcher[MockSpan] {

  override def matchesSafely(mockSpan: MockSpan): Boolean = {
    hasEntry[String, AnyRef]("component", "java-mongo").matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef](is("db.statement"), containsString(operation).asInstanceOf[Matcher[AnyRef]])
        .matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("db.type", "mongo").matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("span.kind", "client").matches(mockSpan.tags()) &&
      operation.equals(mockSpan.operationName())
  }

  override def describeTo(description: Description): Unit = {
    description.appendText("a MongoDB span tagged with operation ")
    description.appendText(operation)
  }
}