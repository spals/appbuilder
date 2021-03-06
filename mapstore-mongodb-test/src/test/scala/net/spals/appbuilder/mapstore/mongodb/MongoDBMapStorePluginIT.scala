package net.spals.appbuilder.mapstore.mongodb

import java.util.Optional

import io.opentracing.mock.{MockSpan, MockTracer}
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions
import net.spals.appbuilder.mapstore.core.model.MultiValueMapRangeKey.in
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.{equalTo => range_equalTo, greaterThan => range_greaterThan, greaterThanOrEqualTo => range_greaterThanOrEqualTo, lessThan => range_lessThan, lessThanOrEqualTo => range_lessThanOrEqualTo}
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.between
import net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.all
import net.spals.appbuilder.mapstore.core.model.{MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.mongodb.MongoDBSpanMatcher.mongoDBSpan
import org.bson.Document
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers._
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

  private val mongoDBTracer = new MockTracer()
  private lazy val mongoClient = {
    val mongoClientProvider = new MongoClientProvider(mongoDBTracer)
    mongoClientProvider.host = System.getenv("MONGODB_IP")
    mongoClientProvider.port = System.getenv("MONGODB_PORT").toInt

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
    groups = Array("MongoDBMapStorePluginIT.empty")
  )
  def testEmptyGetItem(
    tableName: String,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStorePlugin.getItem(tableName, storeKey), is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("find")))
  }

  @Test(
    dataProvider = "emptyGetProvider",
    groups = Array("MongoDBMapStorePluginIT.empty")
  )
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
          .setRange("myRangeField", range_equalTo[String]("myRangeValue1")).build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue1", "key" -> "value").toMap[String, AnyRef].asJava)),
      // Inserted for getItems tests below
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue2")).build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue2", "key" -> "value").toMap[String, AnyRef].asJava)),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue3")).build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue3", "key" -> "value").toMap[String, AnyRef].asJava)),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue4")).build,
        Map("key" -> "value"),
        new Document(Map("myHashField" -> "myHashValue", "myRangeField" -> "myRangeValue4", "key" -> "value").toMap[String, AnyRef].asJava))
    )
  }

  @Test(
    dataProvider = "putItemProvider",
    groups = Array("MongoDBMapStorePluginIT.put"),
    dependsOnGroups = Array("MongoDBMapStorePluginIT.empty")
  )
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

  @Test(
    dataProvider = "updateItemProvider",
    groups = Array("MongoDBMapStorePluginIT.update"),
    dependsOnGroups = Array("MongoDBMapStorePluginIT.put")
  )
  def testUpdateItem(
    payload: Map[String, AnyRef],
    expectedResult: Document
  ) {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", range_equalTo[String]("myRangeValue1")).build

    assertThat(mapStorePlugin.updateItem(rangeTableName, storeKey, payload.asJava).asInstanceOf[Document],
      is(expectedResult))
    assertThat(mapStorePlugin.getItem(rangeTableName, storeKey).asInstanceOf[Optional[Document]],
      is(Optional.of(expectedResult)))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("findandmodify"),
      mongoDBSpan("find")))
  }

  @Test(
    groups = Array("MongoDBMapStorePluginIT.get"),
    dependsOnGroups = Array("MongoDBMapStorePluginIT.put", "MongoDBMapStorePluginIT.update")
  )
  def testGetAllItems() {
    assertThat(mapStorePlugin.getAllItems(rangeTableName).asInstanceOf[java.util.List[Document]],
      // getAllItems isn't ordered on range key
      containsInAnyOrder[Document](result(1), result(2), result(3), result(4)))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("find")))
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
        .setRange("myRangeField", in[String]("myRangeValue2", "myRangeValue3")).build,
        List(result(2), result(3))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_lessThan[String]("myRangeValue3")).build,
        List(result(1), result(2))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_lessThanOrEqualTo[String]("myRangeValue3")).build,
        List(result(1), result(2), result(3)))
    )
  }

  @Test(
    dataProvider = "getItemsProvider",
    groups = Array("MongoDBMapStorePluginIT.get"),
    dependsOnGroups = Array("MongoDBMapStorePluginIT.put", "MongoDBMapStorePluginIT.update")
  )
  def testGetItems(
    storeKey: MapStoreKey,
    expectedResults: List[Document]
  ) {
    assertThat(mapStorePlugin.getItems(rangeTableName, storeKey, defaultOptions()).asInstanceOf[java.util.List[Document]],
      contains[Document](expectedResults: _*))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("find")))
  }

  @Test(
    groups = Array("MongoDBMapStorePluginIT.delete"),
    dependsOnGroups = Array("MongoDBMapStorePluginIT.get")
  )
  def testDeleteItem() {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", range_equalTo[String]("myRangeValue4")).build
    mapStorePlugin.deleteItem(rangeTableName, storeKey)

    assertThat(mapStorePlugin.getAllItems(rangeTableName).asInstanceOf[java.util.List[Document]],
      // getAllItems isn't ordered on range key
      containsInAnyOrder[Document](result(1), result(2), result(3)))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("delete"), mongoDBSpan("find")))
  }

  private def result(i: Int): Document = {
    new Document(Map("myHashField" -> "myHashValue",
      "myRangeField" -> s"myRangeValue$i", "key" -> "value").toMap[String, AnyRef].asJava)
  }
}
