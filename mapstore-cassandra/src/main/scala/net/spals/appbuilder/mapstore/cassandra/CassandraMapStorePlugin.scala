package net.spals.appbuilder.mapstore.cassandra

import java.io.Closeable
import java.util.{Date, Optional, UUID}
import javax.annotation.PreDestroy
import javax.validation.constraints.Min

import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.google.inject.Inject
import com.netflix.governator.annotations
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.mapstore.core.MapStorePlugin
import net.spals.appbuilder.mapstore.core.MapStorePlugin.stripKey
import net.spals.appbuilder.mapstore.core.model.{MapQueryOptions, MapStoreKey, MapStoreTableKey}

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
  * Implementation of [[MapStorePlugin]] which uses Apache Cassandra.
  *
  * NOTE: This uses a schema-less map to hold all row state such
  * that Cassandra is used as a true key-value store. Some consider
  * this an anti-pattern of Cassandra and CQL.
  *
  * TODO: Strongly-typed CassandraStore.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[MapStorePlugin], key = "cassandra")
private[cassandra] class CassandraMapStorePlugin @Inject() (
  @ApplicationName applicationName: String,
  cluster: Cluster
) extends MapStorePlugin with Closeable {

  @annotations.Configuration("mapStore.cassandra.keyspace")
  @volatile
  private[cassandra] var configuredKeyspace: String = null

  @Min(1L)
  @annotations.Configuration("mapStore.cassandra.replicationFactor")
  @volatile
  private[cassandra] var replicationFactor: Int = 2

  private[cassandra] val replicationStrategy: String = "SimpleStrategy"

  private lazy val codecRegistry = new CodecRegistry()
  private lazy val keyspace = Option(configuredKeyspace).getOrElse(applicationName)
  private lazy val session = {
    val replicationOptions = Map[String, AnyRef]("replication_factor" -> Int.box(replicationFactor),
      "class" -> replicationStrategy)
    val createKeyspace = SchemaBuilder.createKeyspace(keyspace).ifNotExists().`with`().durableWrites(true)
        .replication(replicationOptions.asJava)
    val connectedSession = cluster.connect()
    connectedSession.execute(createKeyspace)
    val useKeyspace = s"USE $keyspace"
    connectedSession.execute(useKeyspace)
    connectedSession
  }

  @PreDestroy
  override def close() = {
    // NOTE: Closes *all* sessions created with the cluster
    cluster.close()
  }

  override def createTable(tableName: String, tableKey: MapStoreTableKey): Boolean = {
    val schemaBuilder = SchemaBuilder.createTable(tableName).ifNotExists()
      .addPartitionKey(tableKey.getHashField, loadDataType(tableKey.getHashFieldType))
    tableKey.getRangeField.asScala
      .foreach(schemaBuilder.addClusteringColumn(_, loadDataType(tableKey.getRangeFieldType.get())))
    schemaBuilder.addColumn("payload", DataType.map(DataType.varchar(), DataType.varchar()))

    try {
      session.execute(schemaBuilder.toString).wasApplied()
    } catch {
      case _: RuntimeException => false
    }
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
    val keyValues: List[AnyRef] = key.getRangeField.asScala
      .map(rangeField => List(key.getHashValue, key.getRangeKey.getValue.asInstanceOf[AnyRef]))
      .getOrElse(List[AnyRef](key.getHashValue))

    val queryBuilder = QueryBuilder.insertInto(tableName).values(keyFields.asJava, keyValues.asJava)
      .value("payload", payload)
    session.execute(queryBuilder.toString)
    getItem(tableName, key).get()
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
    row => {
      val cols = row.getColumnDefinitions.asList().asScala
      val rowMap = cols
        .map(col => (col.getName, row.get(col.getName, codecRegistry.codecFor(col.getType))))
        .toMap[String, AnyRef]
      val rowMMap = collection.mutable.Map(rowMap.toSeq: _*)

      val payloadMap = rowMMap.remove("payload").map(_.asInstanceOf[java.util.Map[String, AnyRef]].asScala)
      val keyMap = rowMMap.toMap[String, AnyRef]

      payloadMap.map(_ ++ keyMap).getOrElse(keyMap).asJava
    }
  }
}
