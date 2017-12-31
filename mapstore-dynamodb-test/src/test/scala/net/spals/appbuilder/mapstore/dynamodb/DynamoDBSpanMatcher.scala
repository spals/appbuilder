package net.spals.appbuilder.mapstore.dynamodb

import io.opentracing.mock.MockSpan
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.{Description, TypeSafeMatcher}

/**
  * A Hamcrest [[org.hamcrest.Matcher]] used
  * to match tracing spans specific to DynamoDB.
  *
  * @author tkral
  */
private[dynamodb] object DynamoDBSpanMatcher {

  def dynamoDBSpan(url: String, method: String): DynamoDBSpanMatcher =
    DynamoDBSpanMatcher(url, method)
}

private[dynamodb] case class DynamoDBSpanMatcher(
  url: String,
  method: String
) extends TypeSafeMatcher[MockSpan] {

  override def matchesSafely(mockSpan: MockSpan): Boolean = {
    hasEntry[String, AnyRef]("component", "java-aws-sdk").matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("http.method", method).matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("http.url", url).matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("span.kind", "client").matches(mockSpan.tags()) &&
      "AmazonDynamoDBv2".equals(mockSpan.operationName())
  }

  override def describeTo(description: Description): Unit = {
    description.appendText("a DynamoDB span tagged with method ")
    description.appendText(method)
    description.appendText(" and url ")
    description.appendText(url)
  }
}
