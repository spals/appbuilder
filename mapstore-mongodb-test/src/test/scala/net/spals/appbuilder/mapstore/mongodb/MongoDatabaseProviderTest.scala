package net.spals.appbuilder.mapstore.mongodb

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.is
import org.testng.annotations.{DataProvider, Test}

/**
  * Unit tests for [[MongoDatabaseProvider]].
  *
  * @author tkral
  */
class MongoDatabaseProviderTest {

  @DataProvider def databaseNameProvider(): Array[Array[AnyRef]] = {
    Array(
      // Case: Fallback to application name if no configuration is present
      Array(null, null, "applicationName"),
      // Case: Use explicitly configured database name
      Array("configuredDatabaseName", null, "configuredDatabaseName"),
      // Case: Use database name from URL
      Array(null, "mongodb://host:123/urlDB", "urlDB"),
      // Case: Use database name from URL with collection specified
      Array(null, "mongodb://host:123/urlDB.collection", "urlDB"),
      // Case: Take URL database name over configured database name
      Array("configuredDatabaseName", "mongodb://host:123/urlDB", "urlDB")
    )
  }

  @Test(dataProvider = "databaseNameProvider")
  def testDatabaseName(
    configuredDatabaseName: String,
    url: String,
    expectedDatabaseName: String
  ) {
    val mongoDatabaseProvider = new MongoDatabaseProvider("applicationName", mongoClient = null)
    mongoDatabaseProvider.configuredDatabaseName = configuredDatabaseName
    mongoDatabaseProvider.url = url

    assertThat(mongoDatabaseProvider.databaseName, is(expectedDatabaseName))
  }
}
