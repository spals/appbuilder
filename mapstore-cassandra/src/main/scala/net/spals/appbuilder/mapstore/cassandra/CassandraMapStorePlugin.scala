package net.spals.appbuilder.mapstore.cassandra

import java.io.Closeable
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Date, Optional, UUID}
import javax.annotation.PreDestroy

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.{CodecRegistry, DataType, Row, Session}
import com.google.inject.Inject
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.mapstore.core.MapStorePlugin
import net.spals.appbuilder.mapstore.core.model.{MapQueryOptions, MapStoreKey, MapStoreTableKey}

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
  * Implementation of [[MapStorePlugin]] which
  * uses Apache Cassandra.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MapStorePlugin], key = "cassandra")
private[cassandra] class CassandraMapStorePlugin @Inject() (sessionFuture: FutureTask[Session])
  extends MapStorePlugin with Closeable {

  private lazy val codecRegistry = new CodecRegistry()
  private val sessionInitialized = new AtomicBoolean(false)
  private lazy val session = {
    sessionInitialized.set(true)
    sessionFuture.run()
    sessionFuture.get()
  }

  @PreDestroy
  override def close() = {
    if (sessionInitialized.get()) {
      session.close()
    }
  }

  override def createTable(tableName: String, tableKey: MapStoreTableKey): Boolean = {
    val schemaBuilder = SchemaBuilder.createTable(tableName).ifNotExists()
      .addPartitionKey(tableKey.getHashField, loadDataType(tableKey.getHashFieldType))
    tableKey.getRangeField.asScala
      .foreach(schemaBuilder.addClusteringColumn(_, loadDataType(tableKey.getRangeFieldType.get())))

    session.execute(schemaBuilder.toString).wasApplied()
  }

  override def dropTable(tableName: String): Boolean = {
    val schemaBuilder = SchemaBuilder.dropTable(tableName).ifExists()

    session.execute(schemaBuilder.toString).wasApplied()
  }

  override def deleteItem(tableName: String,
                          key: MapStoreKey): Unit = {
    val keyClause = CassandraKeyClause(key)
    val queryBuilder = QueryBuilder.delete().from(tableName).where(keyClause.hashClause)
    keyClause.rangeClauses.foreach(queryBuilder.and(_))

    session.execute(queryBuilder.toString)
  }

  override def getAllItems(tableName: String): java.util.List[java.util.Map[String, AnyRef]] = {
    val queryBuilder = QueryBuilder.select().all().from(tableName)
    val results = session.execute(queryBuilder.toString).all().asScala
    results.map(rowMapper()).toList.asJava
  }

  override def getItem(tableName: String,
                       key: MapStoreKey): Optional[java.util.Map[String, AnyRef]] = {
    val keyClause = CassandraKeyClause(key)
    val queryBuilder = QueryBuilder.select().all().from(tableName).where(keyClause.hashClause)
    keyClause.rangeClauses.foreach(queryBuilder.and(_))

    val result = session.execute(queryBuilder.toString).one()
    Option(result).map(rowMapper()).asJava
  }

  override def getItems(tableName: String,
                        key: MapStoreKey,
                        options: MapQueryOptions): java.util.List[java.util.Map[String, AnyRef]] = {
    val keyClause = CassandraKeyClause(key)
    val queryBuilder = QueryBuilder.select().all().from(tableName).where(keyClause.hashClause)
    keyClause.rangeClauses.foreach(queryBuilder.and(_))

    key.getRangeField.asScala.map(rangeField => options.getOrder match {
      case MapQueryOptions.Order.ASC => QueryBuilder.asc(rangeField)
      case MapQueryOptions.Order.DESC => QueryBuilder.desc(rangeField)
    }).foreach(queryBuilder.orderBy(_))
    options.getLimit.asScala.foreach(queryBuilder.limit(_))

    val results = session.execute(queryBuilder.toString).all().asScala
    results.map(rowMapper()).toList.asJava
  }

  override def putItem(tableName: String,
                       key: MapStoreKey,
                       payload: java.util.Map[String, AnyRef]): java.util.Map[String, AnyRef] = {
    stripKey(key, payload)

    val keyFields = key.getRangeField.asScala.map(rangeField => List(key.getHashField, rangeField))
      .getOrElse(List(key.getHashField))
    val keyValues = key.getRangeField.asScala.map(rangeField => List(key.getHashValue, key.getRangeKey.getValue))
      .getOrElse(List(key.getHashValue)).asInstanceOf[List[AnyRef]]

    val payloadFields = payload.asScala.keys
    val payloadValues = payload.asScala.values

    val allFields = (keyFields ++ payloadFields).asJava
    val allValues = (keyValues ++ payloadValues).asJava

    val queryBuilder = QueryBuilder.insertInto(tableName).values(allFields, allValues)
    rowMapper().apply(session.execute(queryBuilder.toString).one())
  }

  override def updateItem(tableName: String,
                          key: MapStoreKey,
                          payload: java.util.Map[String, AnyRef]): java.util.Map[String, AnyRef] = {
    val keyClause = CassandraKeyClause(key)
    val queryBuilder = QueryBuilder.update(tableName).where(keyClause.hashClause)
    keyClause.rangeClauses.foreach(queryBuilder.and(_))

    stripKey(key, payload)
    val assignments = payload.asScala.map(entry => QueryBuilder.set(entry._1, entry._2))
    assignments.foreach(queryBuilder.`with`(_))

    rowMapper().apply(session.execute(queryBuilder.toString).one())
  }

  private[cassandra] def loadDataType(fieldType: Class[_]): DataType = {
    fieldType match {
      case fType if fType.equals(classOf[Boolean]) || fType.equals(classOf[java.lang.Boolean]) => DataType.cboolean()
      case fType if fType.equals(classOf[Byte]) || fType.equals(classOf[java.lang.Byte]) => DataType.tinyint()
      case fType if fType.equals(classOf[Date]) => DataType.date()
      case fType if fType.equals(classOf[Double]) || fType.equals(classOf[java.lang.Double]) => DataType.cdouble()
      case fType if fType.equals(classOf[Float]) || fType.equals(classOf[java.lang.Float]) => DataType.cfloat()
      case fType if fType.equals(classOf[Int]) || fType.equals(classOf[java.lang.Integer]) => DataType.cint()
      case fType if fType.equals(classOf[Long]) || fType.equals(classOf[java.lang.Long]) => DataType.bigint()
      case fType if fType.equals(classOf[Short]) || fType.equals(classOf[java.lang.Short]) => DataType.smallint()
      case fType if fType.equals(classOf[String]) => DataType.varchar()
      case fType if fType.equals(classOf[UUID]) => DataType.uuid()
      case _ => throw new IllegalArgumentException(s"Unsupported Cassandra column DataType: $fieldType")
    }
  }

  private[cassandra] def rowMapper(): Row => java.util.Map[String, AnyRef] = {
    row => row.getColumnDefinitions.asList().asScala
      .map(col => (col.getName, row.get(col.getName, codecRegistry.codecFor(col.getType))))
      .toMap[String, AnyRef].asJava
  }
}
