package net.spals.appbuilder.mapstore.dynamodb

import java.util.Optional

import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey._
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.between
import net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.all
import net.spals.appbuilder.mapstore.core.model.{MapStoreKey, MapStoreTableKey, TwoValueMapRangeKey, ZeroValueMapRangeKey}
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{contains, empty, is}
import org.testng.annotations.{AfterClass, BeforeClass, DataProvider, Test}

import scala.collection.JavaConverters._

/**
  * Integration tests for [[DynamoDBMapStorePlugin]].
  *
  * @author tkral
  */
class DynamoDBMapStorePluginIT {

  private lazy val dynamoDBClient = {
    val dynamoDBClientProvider = new DynamoDBClientProvider
    dynamoDBClientProvider.awsAccessKeyId = "DUMMY"
    dynamoDBClientProvider.awsSecretKey = "DUMMY"
    dynamoDBClientProvider.endpoint = s"http://10.0.1.24:${System.getenv("DYNAMODB_LOCAL_PORT")}"

    dynamoDBClientProvider.get()
  }

  private lazy val storePlugin = new DynamoDBMapStorePlugin(dynamoDBClient)

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
    storePlugin.createTable(hashTableName, hashTableKey)
    storePlugin.createTable(rangeTableName, rangeTableKey)
  }

  @AfterClass(alwaysRun = true) def dropTables() {
    storePlugin.dropTable(hashTableName)
    storePlugin.dropTable(rangeTableName)
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
    assertThat(storePlugin.getItem(tableName, storeKey), is(Optional.empty[java.util.Map[String, AnyRef]]))
  }

  @Test(dataProvider = "emptyGetProvider")
  def testEmptyGetItems(tableName: String,
                        storeKey: MapStoreKey) {
    assertThat(storePlugin.getItems(tableName, storeKey, defaultOptions()), empty[java.util.Map[String, AnyRef]])
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
    assertThat(storePlugin.putItem(tableName, storeKey, payload.asJava), is(expectedResult.asJava))
    assertThat(storePlugin.getItem(tableName, storeKey), is(Optional.of(expectedResult.asJava)))
  }

  @DataProvider def updateItemProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(Map("key1" -> Long.box(1L)),
        Map("myHashField" -> "myHashValue1", "myRangeField" -> "myRangeValue1", "key" -> "value", "key1" -> Long.box(1L))),
      Array(Map("key1" -> ""),
        Map("myHashField" -> "myHashField1", "myRangeField" -> "myRangeValue1", "key" -> "value"))
    )
  }

  @Test(enabled = false, dataProvider = "updateItemProvider", dependsOnMethods = Array("testPutItem"))
  def testUpdateItem(payload: Map[String, AnyRef],
                     expectedResult: Map[String, AnyRef]) {
    val storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue1")
      .setRange("myRangeField", equalTo[String]("myRangeValue1")).build

    assertThat(storePlugin.updateItem(rangeTableName, storeKey, payload.asJava), is(expectedResult.asJava))
    assertThat(storePlugin.getItem(rangeTableName, storeKey), is(Optional.of(expectedResult.asJava)))
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
        List(result(1), result(2), result(3)))
    )
  }

  @Test(dataProvider = "getItemsProvider", dependsOnMethods = Array("testPutItem"))
  def testGetItems(storeKey: MapStoreKey,
                   expectedResults: List[Map[String, AnyRef]]) {
    assertThat(storePlugin.getItems(rangeTableName, storeKey, defaultOptions()),
      contains[java.util.Map[String, AnyRef]](expectedResults.map(_.asJava): _*))
  }
}
