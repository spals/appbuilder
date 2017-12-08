package net.spals.appbuilder.mapstore.cassandra

import java.util.Optional

import io.opentracing.mock.{MockSpan, MockTracer}
import net.spals.appbuilder.mapstore.cassandra.CassandraSpanMatcher.cassandraSpan
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.{equalTo => range_equalTo, greaterThan => range_greaterThan, greaterThanOrEqualTo => range_greaterThanOrEqualTo, lessThan => range_lessThan, lessThanOrEqualTo => range_lessThanOrEqualTo}
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.between
import net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.all
import net.spals.appbuilder.mapstore.core.model.{MapStoreKey, MapStoreTableKey}
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers._
import org.hamcrest.{Description, TypeSafeMatcher}
import org.testng.annotations._

import scala.collection.JavaConverters._

/**
  * Integration tests for [[CassandraMapStorePlugin]].
  *
  * @author tkral
  */
class CassandraMapStorePluginIT {

  private lazy val clusterInitializer = {
    val initializerProvider = new CassandraClusterInitializerProvider()
    initializerProvider.clusterName = "CassandraMapStorePluginIT"
    initializerProvider.hosts = System.getenv("CASSANDRA_IP")
    initializerProvider.port = System.getenv("CASSANDRA_PORT").toInt
    initializerProvider.get()
  }

  private val cassandraTracer = new MockTracer()
  private lazy val cluster = {
    val clusterProvider = new CassandraClusterProvider(clusterInitializer, cassandraTracer)
    clusterProvider.get()
  }

  private val applicationName = "CassandraMapStorePluginIT"
  private lazy val mapStorePlugin = new CassandraMapStorePlugin(applicationName, cluster)

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
    cassandraTracer.reset()
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
          .setRange("myRangeField", range_equalTo[String]("deadbeef")).build)
    )
  }

  @Test(dataProvider = "emptyGetProvider", groups = Array("empty"))
  def testEmptyGetItem(
    tableName: String,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStorePlugin.getItem(tableName, storeKey), is(Optional.empty[java.util.Map[String, AnyRef]]))
    assertThat(cassandraTracer.finishedSpans(), contains[MockSpan](
      cassandraSpan(applicationName.toLowerCase, "SELECT")
    ))
  }

  @Test(dataProvider = "emptyGetProvider", groups = Array("empty"))
  def testEmptyGetItems(
    tableName: String,
    storeKey: MapStoreKey
  ) {
    assertThat(mapStorePlugin.getItems(tableName, storeKey, defaultOptions()), empty[java.util.Map[String, AnyRef]])
    assertThat(cassandraTracer.finishedSpans(), contains[MockSpan](
      cassandraSpan(applicationName.toLowerCase, "SELECT")
    ))
  }

  @DataProvider def putItemProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(hashTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue").build,
        Map("key" -> "value"),
        // NOTE: Cassandra keys are case insensitive
        Map("myhashfield" -> "myHashValue", "key" -> "value")),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue1")).build,
        Map("key" -> "value"),
        Map("myhashfield" -> "myHashValue", "myrangefield" -> "myRangeValue1", "key" -> "value")),
      // Inserted for getItems tests below
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue2")).build,
        Map("key" -> "value"),
        Map("myhashfield" -> "myHashValue", "myrangefield" -> "myRangeValue2", "key" -> "value")),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue3")).build,
        Map("key" -> "value"),
        Map("myhashfield" -> "myHashValue", "myrangefield" -> "myRangeValue3", "key" -> "value")),
      Array(rangeTableName,
        new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
          .setRange("myRangeField", range_equalTo[String]("myRangeValue4")).build,
        Map("key" -> "value"),
        Map("myhashfield" -> "myHashValue", "myrangefield" -> "myRangeValue4", "key" -> "value"))
    )
  }

  @Test(dataProvider = "putItemProvider", groups = Array("put"), dependsOnGroups = Array("empty"))
  def testPutItem(
    tableName: String,
    storeKey: MapStoreKey,
    payload: Map[String, AnyRef],
    expectedResult: Map[String, AnyRef]
  ) {
    assertThat(mapStorePlugin.putItem(tableName, storeKey, payload.asJava), is(expectedResult.asJava))
    assertThat(mapStorePlugin.getItem(tableName, storeKey), is(Optional.of(expectedResult.asJava)))
    assertThat(cassandraTracer.finishedSpans(), contains[MockSpan](
      cassandraSpan(applicationName.toLowerCase, "INSERT"),
      cassandraSpan(applicationName.toLowerCase, "SELECT"),
      cassandraSpan(applicationName.toLowerCase, "SELECT")
    ))
  }

  @DataProvider def updateItemProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(Map("numberKey" -> Long.box(1L)),
        // NOTE: Cassandra keys are case insensitive
        Map("myhashfield" -> "myHashValue", "myrangefield" -> "myRangeValue1", "key" -> "value", "numberKey" -> java.math.BigDecimal.valueOf(1L))),
      Array(Map("numberKey" -> ""),
        Map("myhashfield" -> "myHashValue", "myrangefield" -> "myRangeValue1", "key" -> "value"))
    )
  }

  @Test(enabled = false, dataProvider = "updateItemProvider", groups = Array("update"), dependsOnGroups = Array("put"))
  def testUpdateItem(
    payload: Map[String, AnyRef],
    expectedResult: Map[String, AnyRef]
  ) {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", range_equalTo[String]("myRangeValue1")).build

    assertThat(mapStorePlugin.updateItem(rangeTableName, storeKey, payload.asJava), is(expectedResult.asJava))
    assertThat(mapStorePlugin.getItem(rangeTableName, storeKey), is(Optional.of(expectedResult.asJava)))
    assertThat(cassandraTracer.finishedSpans(), contains[MockSpan](
      cassandraSpan(applicationName.toLowerCase, "UPDATE"),
      cassandraSpan(applicationName.toLowerCase, "SELECT")
    ))
  }

  @Test(groups = Array("get"), dependsOnGroups = Array("put"/*, "update"*/))
  def testGetAllItems() {
    assertThat(mapStorePlugin.getAllItems(rangeTableName),
      // getAllItems isn't ordered on range key
      containsInAnyOrder[java.util.Map[String, AnyRef]](result(1).asJava, result(2).asJava, result(3).asJava, result(4).asJava))
    assertThat(cassandraTracer.finishedSpans(), contains[MockSpan](
      cassandraSpan(applicationName.toLowerCase, "SELECT")
    ))
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
//      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
//        .setRange("myRangeField", in[String]("myRangeValue2", "myRangeValue4")).build,
//        List(result(2), result(4))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_lessThan[String]("myRangeValue3")).build,
        List(result(1), result(2))),
      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
        .setRange("myRangeField", range_lessThanOrEqualTo[String]("myRangeValue3")).build,
        List(result(1), result(2), result(3)))//,
//      Array(new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
//        .setRange("myRangeField", startsWith("myRangeValue")),
//        List(result(1), result(2), result(3), result(4)))
    )
  }

  @Test(dataProvider = "getItemsProvider", groups = Array("get"), dependsOnGroups = Array("put"/*, "update"*/))
  def testGetItems(
    storeKey: MapStoreKey,
    expectedResults: List[Map[String, AnyRef]]
  ) {
    assertThat(mapStorePlugin.getItems(rangeTableName, storeKey, defaultOptions()),
      contains[java.util.Map[String, AnyRef]](expectedResults.map(_.asJava): _*))
    assertThat(cassandraTracer.finishedSpans(), contains[MockSpan](
      cassandraSpan(applicationName.toLowerCase, "SELECT")
    ))
  }

  @Test(groups = Array("delete"), dependsOnGroups = Array("get"))
  def testDeleteItem() {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", range_equalTo[String]("myRangeValue4")).build
    mapStorePlugin.deleteItem(rangeTableName, storeKey)

    assertThat(mapStorePlugin.getAllItems(rangeTableName),
      // getAllItems isn't ordered on range key
      containsInAnyOrder[java.util.Map[String, AnyRef]](result(1).asJava, result(2).asJava, result(3).asJava))
    assertThat(cassandraTracer.finishedSpans(), contains[MockSpan](
      cassandraSpan(applicationName.toLowerCase, "DELETE"), cassandraSpan(applicationName.toLowerCase, "SELECT")
    ))
  }

  private def result(i: Int): Map[String, AnyRef] = {
    Map("myhashfield" -> "myHashValue", "myrangefield" -> s"myRangeValue$i", "key" -> "value")
  }
}

private object CassandraSpanMatcher {

  def cassandraSpan(dbInstance: String, dbStatementOp: String): CassandraSpanMatcher =
    CassandraSpanMatcher(dbInstance, dbStatementOp)
}

private case class CassandraSpanMatcher(
  dbInstance: String,
  dbStatementOp: String
) extends TypeSafeMatcher[MockSpan] {

  override def matchesSafely(mockSpan: MockSpan): Boolean = {
    hasEntry[String, AnyRef]("component", "java-cassandra").matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("db.instance", dbInstance).matches(mockSpan.tags()) &&
      hasEntry[String, String](is("db.statement"), startsWith(dbStatementOp)).matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("db.type", "cassandra").matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("span.kind", "client").matches(mockSpan.tags()) &&
      hasKey[String]("peer.hostname").matches(mockSpan.tags()) &&
      hasKey[String]("peer.port").matches(mockSpan.tags()) &&
      "execute".equals(mockSpan.operationName())
  }

  override def describeTo(description: Description): Unit = {
    description.appendText("a Cassandra span tagged with db instance ")
    description.appendText(dbInstance)
  }
}
