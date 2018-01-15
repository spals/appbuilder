package net.spals.appbuilder.mapstore.mongodb

import java.util.Optional

import com.google.common.collect.ImmutableMap
import io.opentracing.mock.{MockSpan, MockTracer}
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions
import net.spals.appbuilder.mapstore.core.model.MapStoreIndexName.indexName
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.{equalTo => range_equalTo}
import net.spals.appbuilder.mapstore.core.model.{MapStoreIndexName, MapStoreKey, MapStoreTableKey}
import net.spals.appbuilder.mapstore.mongodb.MongoDBSpanMatcher.mongoDBSpan
import org.bson.Document
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers._
import org.slf4j.LoggerFactory
import org.testng.annotations._

import scala.collection.JavaConverters._

/**
  * Integration tests for [[MongoDBMapStoreIndexPlugin]].
  *
  * @author tkral
  */
class MongoDBMapStoreIndexPluginIT {
  private val LOGGER = LoggerFactory.getLogger(classOf[MongoDBMapStoreIndexPluginIT])

  private val mongoDBTracer = new MockTracer()
  private lazy val mongoClient = {
    val mongoClientProvider = new MongoClientProvider(mongoDBTracer)
    mongoClientProvider.host = System.getenv("MONGODB_IP")
    mongoClientProvider.port = System.getenv("MONGODB_PORT").toInt

    LOGGER.info(s"Connecting to mongoDB instance at ${mongoClientProvider.host}:${mongoClientProvider.port}")
    mongoClientProvider.get()
  }

  private val applicationName = "MongoDBMapStoreIndexPluginIT"
  private lazy val mongoDatabase = {
    val mongoDatabaseProvider = new MongoDatabaseProvider(applicationName, mongoClient)
    mongoDatabaseProvider.get()
  }
  private lazy val mapStorePlugin = new MongoDBMapStorePlugin(mongoClient, mongoDatabase)
  private lazy val mapStoreIndexPlugin = new MongoDBMapStoreIndexPlugin(mongoDatabase, mapStorePlugin)

  private val hashIndexName = indexName("hashTable", "hashIndex")
  private val hashTableKey = new MapStoreTableKey.Builder()
    .setHash("tableHashField", classOf[String])
    .build
  private val hashIndexKey = new MapStoreTableKey.Builder()
    .setHash("indexHashField", classOf[String])
    .build

  private val rangeIndexName = indexName("rangeTable", "rangeIndex")
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
    mongoDBTracer.reset()
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
    groups = Array("MongoDBMapStoreIndexPluginIT.empty")
  )
  def testEmptyGetItem(
    indexName: MapStoreIndexName,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStoreIndexPlugin.getItem(indexName, storeKey), is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("find")))
  }

  @Test(
    dataProvider = "emptyGetProvider",
    groups = Array("MongoDBMapStoreIndexPluginIT.empty")
  )
  def testEmptyGetItems(
    indexName: MapStoreIndexName,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStoreIndexPlugin.getItems(indexName, storeKey, defaultOptions()), empty[java.util.Map[String, AnyRef]])
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](mongoDBSpan("find")))
  }

  @Test(
    groups = Array("MongoDBMapStoreIndexPluginIT.get"),
    dependsOnGroups = Array("MongoDBMapStoreIndexPluginIT.empty")
  )
  def testGetItemHashOnly() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build
    mapStorePlugin.putItem(hashIndexName.getTableName, tableItemKey,
      Map("indexHashField" -> "index").asJava.asInstanceOf[java.util.Map[String, AnyRef]])

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build
    val indexItem = mapStoreIndexPlugin.getItem(hashIndexName, indexItemKey)
    assertThat(indexItem, not(Optional.empty[java.util.Map[String, AnyRef]]))

    val itemMatcher: Matcher[Document] =
      equalTo(new Document(ImmutableMap.of("tableHashField", "table", "indexHashField", "index")))
    assertThat(indexItem.get.asInstanceOf[Document], itemMatcher)
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](
      mongoDBSpan("insert"),
      mongoDBSpan("find")
    ))
  }

  @Test(
    groups = Array("MongoDBMapStoreIndexPluginIT.get"),
    dependsOnGroups = Array("MongoDBMapStoreIndexPluginIT.empty")
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

    val itemMatcher: Matcher[Document] =
      equalTo(new Document(ImmutableMap.of(
        "tableHashField", "table", "tableRangeField", "1",
        "indexHashField", "index", "indexRangeField", "11"
      )))
    assertThat(indexItem.get.asInstanceOf[Document], itemMatcher)
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](
      mongoDBSpan("insert"),
      mongoDBSpan("find")
    ))
  }

  @Test(
    groups = Array("MongoDBMapStoreIndexPluginIT.update"),
    dependsOnGroups = Array("MongoDBMapStoreIndexPluginIT.get")
  )
  def testUpdateItemHashOnly() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build
    mapStorePlugin.updateItem(hashIndexName.getTableName, tableItemKey,
      Map("key" -> "value").asJava.asInstanceOf[java.util.Map[String, AnyRef]])

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build
    val indexItem = mapStoreIndexPlugin.getItem(hashIndexName, indexItemKey)
    assertThat(indexItem, not(Optional.empty[java.util.Map[String, AnyRef]]))

    val itemMatcher: Matcher[java.util.Map[String, AnyRef]] =
      equalTo(new Document(ImmutableMap.of(
        "tableHashField", "table", "indexHashField", "index",
        "key", "value"
      )))
    assertThat(indexItem.get.asInstanceOf[Document], itemMatcher)
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](
      mongoDBSpan("findandmodify"),
      mongoDBSpan("find")
    ))
  }

  @Test(
    groups = Array("MongoDBMapStoreIndexPluginIT.update"),
    dependsOnGroups = Array("MongoDBMapStoreIndexPluginIT.get")
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

    val itemMatcher: Matcher[Document] =
      equalTo(new Document(ImmutableMap.of(
        "tableHashField", "table", "tableRangeField", "1",
        "indexHashField", "index", "indexRangeField", "11",
        "key", "value"
      )))
    assertThat(indexItem.get.asInstanceOf[Document], itemMatcher)
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](
      mongoDBSpan("findandmodify"),
      mongoDBSpan("find")
    ))
  }

  @Test(
    groups = Array("MongoDBMapStoreIndexPluginIT.delete"),
    dependsOnGroups = Array("MongoDBMapStoreIndexPluginIT.update")
  )
  def testDeleteItemHashOnly() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build
    mapStorePlugin.deleteItem(hashIndexName.getTableName, tableItemKey)

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build
    val indexItem = mapStoreIndexPlugin.getItem(hashIndexName, indexItemKey)
    assertThat(indexItem, is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](
      mongoDBSpan("delete"),
      mongoDBSpan("find")
    ))
  }

  @Test(
    groups = Array("MongoDBMapStoreIndexPluginIT.delete"),
    dependsOnGroups = Array("MongoDBMapStoreIndexPluginIT.update")
  )
  def testDeleteItemHashRange() {
    val tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table")
      .setRange("tableRangeField", range_equalTo[String]("1")).build
    mapStorePlugin.deleteItem(rangeIndexName.getTableName, tableItemKey)

    val indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index")
      .setRange("indexRangeField", range_equalTo[String]("11")).build
    val indexItem = mapStoreIndexPlugin.getItem(rangeIndexName, indexItemKey)
    assertThat(indexItem, is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(mongoDBTracer.finishedSpans(), contains[MockSpan](
      mongoDBSpan("delete"),
      mongoDBSpan("find")
    ))
  }
}
