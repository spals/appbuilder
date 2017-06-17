package net.spals.appbuilder.mapstore.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.is
import org.mockito.Mockito.mock
import org.testng.annotations.{DataProvider, Test}

/**
  * Unit tests for [[DynamoDBMapStorePlugin]]
  *
  * @author tkral
  */
class DynamoDBMapStorePluginTest {

  @DataProvider def createAttributeTypeProvider(): Array[Array[AnyRef]] = {
    Array(
      Array(classOf[AnyRef], ScalarAttributeType.S),
      Array(classOf[Boolean], ScalarAttributeType.B),
      Array(classOf[Byte], ScalarAttributeType.N),
      Array(classOf[Double], ScalarAttributeType.N),
      Array(classOf[Float], ScalarAttributeType.N),
      Array(classOf[Int], ScalarAttributeType.N),
      Array(classOf[Long], ScalarAttributeType.N),
      Array(classOf[Short], ScalarAttributeType.N),
      Array(classOf[String], ScalarAttributeType.S),

      Array(classOf[java.math.BigDecimal], ScalarAttributeType.N),
      Array(classOf[java.math.BigInteger], ScalarAttributeType.N),
      Array(classOf[java.lang.Boolean], ScalarAttributeType.B),
      Array(classOf[java.lang.Byte], ScalarAttributeType.N),
      Array(classOf[java.lang.Double], ScalarAttributeType.N),
      Array(classOf[java.lang.Float], ScalarAttributeType.N),
      Array(classOf[java.lang.Integer], ScalarAttributeType.N),
      Array(classOf[java.lang.Long], ScalarAttributeType.N),
      Array(classOf[java.lang.Object], ScalarAttributeType.S),
      Array(classOf[java.lang.Short], ScalarAttributeType.N),
      Array(classOf[java.lang.String], ScalarAttributeType.S)
    )
  }

  @Test(dataProvider = "createAttributeTypeProvider")
  def testCreateAttributeType(fieldType: Class[_], expectedAttributeType: ScalarAttributeType) {
    val dynamoDBMapStorePlugin = new DynamoDBMapStorePlugin(mock(classOf[AmazonDynamoDB]))
    assertThat(dynamoDBMapStorePlugin.createAttributeType(fieldType), is(expectedAttributeType))
  }
}
