package net.spals.appbuilder.mapstore.cassandra

import io.opentracing.mock.MockSpan
import org.hamcrest.Matchers.{hasEntry, hasKey, is, startsWith}
import org.hamcrest.{Description, TypeSafeMatcher}

/**
  * A Hamcrest [[org.hamcrest.Matcher]] used
  * to match tracing spans specific to Cassandra.
  *
  * @author tkral
  */
private[cassandra] object CassandraSpanMatcher {

  def cassandraSpan(dbInstance: String, dbStatementOp: String): CassandraSpanMatcher =
    CassandraSpanMatcher(dbInstance, dbStatementOp)
}

private[cassandra] case class CassandraSpanMatcher(
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
