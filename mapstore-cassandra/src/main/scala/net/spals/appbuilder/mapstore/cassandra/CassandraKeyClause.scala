package net.spals.appbuilder.mapstore.cassandra

import com.datastax.driver.core.querybuilder.{Clause, QueryBuilder}
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.{Extended, Standard}
import net.spals.appbuilder.mapstore.core.model.MapStoreKey
import net.spals.appbuilder.mapstore.core.model.MultiValueMapRangeKey.ListValueHolder
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.TwoValueHolder

import scala.compat.java8.OptionConverters._

/**
  * Translation object between [[MapStoreKey]]s and
  * Cassandra query [[Clause]]s
  *
  * @author tkral
  */
private[cassandra] case class CassandraKeyClause(key: MapStoreKey) {

  def hashClause: Clause = QueryBuilder.eq(key.getHashField, key.getHashValue)

  def rangeClauses: List[Clause] = {
    (key.getRangeField.asScala, key.getRangeKey.getOperator, key.getRangeKey.getValue) match {
      case (None, _, _) => List.empty[Clause]
      case (_, Standard.ALL, _) => List.empty[Clause]
      case (_, Standard.NONE, _) => List.empty[Clause]
      case (Some(rField), Standard.BETWEEN, rValue) =>
        List(QueryBuilder.gte(rField, rValue.asInstanceOf[TwoValueHolder[_]].getValue1),
          QueryBuilder.lte(rField, rValue.asInstanceOf[TwoValueHolder[_]].getValue2))
      case (Some(rField), Standard.EQUAL_TO, rValue) => List(QueryBuilder.eq(rField, rValue))
      case (Some(rField), Standard.GREATER_THAN, rValue) => List(QueryBuilder.gt(rField, rValue))
      case (Some(rField), Standard.GREATER_THAN_OR_EQUAL_TO, rValue) => List(QueryBuilder.gte(rField, rValue))
      case (Some(rField), Standard.LESS_THAN, rValue) => List(QueryBuilder.lt(rField, rValue))
      case (Some(rField), Standard.LESS_THAN_OR_EQUAL_TO, rValue) => List(QueryBuilder.lte(rField, rValue))
      case (Some(rField), Extended.IN, rValue) =>
        List(QueryBuilder.in(rField, rValue.asInstanceOf[ListValueHolder[_]].getValues))
      case (Some(rField), Extended.LIKE, rValue) => List(QueryBuilder.like(rField, s"%$rValue%"))
      case (Some(rField), Extended.STARTS_WITH, rValue) => List(QueryBuilder.like(rField, s"$rValue%"))
      case (_, operator, _) =>
        throw new IllegalArgumentException(s"Cassandra cannot support the operator $operator")
    }
  }
}
