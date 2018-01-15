package net.spals.appbuilder.mapstore.mongodb

import io.opentracing.mock.MockSpan
import org.hamcrest.Matchers.{containsString, hasEntry, is}
import org.hamcrest.{Description, Matcher, TypeSafeMatcher}

/**
  * A Hamcrest [[org.hamcrest.Matcher]] used
  * to match tracing spans specific to MongoDB.
  *
  * @author tkral
  */
private[mongodb] object MongoDBSpanMatcher {

  def mongoDBSpan(operation: String): MongoDBSpanMatcher =
    MongoDBSpanMatcher(operation)
}

private[mongodb] case class MongoDBSpanMatcher(
  operation: String
) extends TypeSafeMatcher[MockSpan] {

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
