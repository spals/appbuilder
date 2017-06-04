package net.spals.appbuilder.mapstore.cassandra

import com.datastax.driver.core.querybuilder.{QueryBuilder, Select}
import net.spals.appbuilder.mapstore.core.model.MultiValueMapRangeKey.in
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey._
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.between
import net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey._
import net.spals.appbuilder.mapstore.core.model.{MapRangeKey, MapStoreKey, MultiValueMapRangeKey}
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasToString
import org.testng.annotations.{DataProvider, Test}

/**
  * Unit tests for [[CassandraKeyClause]].
  *
  * @author tkral
  */
class CassandraKeyClauseTest {

  @Test def testHashClause() {
    val key = new MapStoreKey.Builder().setHash("myHashField", "myHashValue").build
    val hashClause = CassandraKeyClause(key).hashClause
    val queryBuilder = QueryBuilder.select().all().from("myTable").where(hashClause)

    assertThat(queryBuilder,
      hasToString[Select.Where]("SELECT * FROM myTable WHERE myHashField='myHashValue';"))
  }

  @DataProvider def rangeClausesProvider(): Array[Array[AnyRef]] = {
    Array(
      // All and none range key operators do not produce a range clause
      Array(all(), ""),
      Array(none(), ""),
      // Cases: Single value operators
      Array(equalTo[String]("myRangeValue"), " AND myRangeField='myRangeValue'"),
      Array(greaterThan[String]("myRangeValue"), " AND myRangeField>'myRangeValue'"),
      Array(greaterThanOrEqualTo[String]("myRangeValue"), " AND myRangeField>='myRangeValue'"),
      Array(lessThan[String]("myRangeValue"), " AND myRangeField<'myRangeValue'"),
      Array(lessThanOrEqualTo[String]("myRangeValue"), " AND myRangeField<='myRangeValue'"),
      Array(like("myRangeValue"), " AND myRangeField LIKE '%myRangeValue%'"),
      Array(startsWith("myRangeValue"), " AND myRangeField LIKE 'myRangeValue%'"),
      // Cases: Two value operators
      Array(between[String]("myRangeValue1", "myRangeValue10"),
        " AND myRangeField>='myRangeValue1' AND myRangeField<='myRangeValue10'")//,
      // Cases: Multi value operators
//      Array(in[String]("myRangeValue1", List("myRangeValue2"): _*),
//        " AND myRangeField IN ('myRangeValue1','myRangeValue2')")
    )
  }

  @Test(dataProvider = "rangeClausesProvider")
  def testRangeClauses(rangeKey: MapRangeKey[String], expectedRangeClause: String) {
    val key = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
      .setRange("myRangeField", rangeKey).build
    val hashClause = CassandraKeyClause(key).hashClause
    val queryBuilder = QueryBuilder.select().all().from("myTable").where(hashClause)

    val rangeClauses = CassandraKeyClause(key).rangeClauses
    rangeClauses.foreach(queryBuilder.and(_))

    assertThat(queryBuilder,
      hasToString[Select.Where](s"SELECT * FROM myTable WHERE myHashField='myHashValue'$expectedRangeClause;"))
  }
}
