package net.spals.appbuilder.mapstore.dynamodb

import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.{PrimaryKey, RangeKeyCondition}
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import com.google.common.annotations.VisibleForTesting
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.{Extended, Standard}
import net.spals.appbuilder.mapstore.core.model.{MapQueryOptions, MapStoreKey}
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.TwoValueHolder

import scala.compat.java8.OptionConverters._

/**
  * Shared utilities for [[DynamoDBMapStorePlugin]]
  * and [[DynamoDBMapStoreIndexPlugin]].
  *
  * @author tkral
  */
private[dynamodb] object DynamoDBMapStoreUtil {

  private[dynamodb] def createAttributeType(fieldType: Class[_]): ScalarAttributeType = {
    fieldType match {
      case booleanType if booleanType.equals(classOf[Boolean]) => ScalarAttributeType.B
      case byteType if byteType.equals(classOf[Byte]) => ScalarAttributeType.N
      case doubleType if doubleType.equals(classOf[Double]) => ScalarAttributeType.N
      case floatType if floatType.equals(classOf[Float]) => ScalarAttributeType.N
      case intType if intType.equals(classOf[Int]) => ScalarAttributeType.N
      case javaBooleanType if javaBooleanType.equals(classOf[java.lang.Boolean]) => ScalarAttributeType.B
      case javaNumberType if classOf[java.lang.Number].isAssignableFrom(javaNumberType) => ScalarAttributeType.N
      case longType if longType.equals(classOf[Long]) => ScalarAttributeType.N
      case shortType if shortType.equals(classOf[Short]) => ScalarAttributeType.N
      case _ => ScalarAttributeType.S
    }
  }

  @VisibleForTesting
  private[dynamodb] def createPrimaryKey(key: MapStoreKey): PrimaryKey = {
    key.getRangeField.asScala
      .map(rangeField => new PrimaryKey(key.getHashField, key.getHashValue, rangeField, key.getRangeKey.getValue))
      .getOrElse(new PrimaryKey(key.getHashField, key.getHashValue))
  }

  @VisibleForTesting
  private[dynamodb] def createQuerySpec(
    key: MapStoreKey,
    options: MapQueryOptions
  ): QuerySpec = {
    val querySpec = new QuerySpec().withHashKey(key.getHashField, key.getHashValue)
    createRangeKeyCondition(key).foreach(rangeKeyCondition => querySpec.withRangeKeyCondition(rangeKeyCondition))

    querySpec.withScanIndexForward(options.getOrder == MapQueryOptions.Order.ASC)
    options.getLimit.asScala.foreach(limit => querySpec.withMaxResultSize(limit))
    querySpec
  }

  @VisibleForTesting
  private[dynamodb] def createRangeKeyCondition(key: MapStoreKey): Option[RangeKeyCondition] = {
    key.getRangeField.asScala.flatMap(rangeField => {
      val rangeKeyCondition = new RangeKeyCondition(rangeField)
      (key.getRangeKey.getOperator, key.getRangeKey.getValue) match {
        case (Standard.ALL, _) => Option.empty[RangeKeyCondition]
        case (Standard.NONE, _) => Option.empty[RangeKeyCondition]
        case (Standard.BETWEEN, rValue) =>
          Option(rangeKeyCondition.between(rValue.asInstanceOf[TwoValueHolder[_]].getValue1,
            rValue.asInstanceOf[TwoValueHolder[_]].getValue2))
        case (Standard.EQUAL_TO, rValue) => Option(rangeKeyCondition.eq(rValue))
        case (Standard.GREATER_THAN, rValue) => Option(rangeKeyCondition.gt(rValue))
        case (Standard.GREATER_THAN_OR_EQUAL_TO, rValue) => Option(rangeKeyCondition.ge(rValue))
        case (Standard.LESS_THAN, rValue) => Option(rangeKeyCondition.lt(rValue))
        case (Standard.LESS_THAN_OR_EQUAL_TO, rValue) => Option(rangeKeyCondition.le(rValue))
        case (Extended.STARTS_WITH, rValue) => Option(rangeKeyCondition.beginsWith(rValue.asInstanceOf[String]))
        case (operator, _) =>
          throw new IllegalArgumentException(s"DynamoDB cannot support the operator $operator")
      }
    })
  }
}
