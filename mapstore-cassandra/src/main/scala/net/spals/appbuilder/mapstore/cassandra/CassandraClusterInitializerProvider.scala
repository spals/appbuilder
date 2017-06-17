package net.spals.appbuilder.mapstore.cassandra

import javax.validation.constraints.{Max, Min, NotNull}

import com.datastax.driver.core.Cluster.Initializer
import com.datastax.driver.core.Host.StateListener
import com.datastax.driver.core.{Cluster, Host, ProtocolOptions}
import com.google.inject.Provider
import com.netflix.governator.annotations
import net.spals.appbuilder.annotations.service.{AutoBindProvider, AutoBindSingleton}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Cassandra cluster initializer which reads
  * from the appbuilder service configuration.
  *
  * @author tkral
  */
@AutoBindProvider
private[cassandra] class CassandraClusterInitializerProvider
  extends Provider[Initializer] with StateListener {

  private val LOGGER = LoggerFactory.getLogger(classOf[CassandraClusterInitializerProvider])

  @NotNull
  @annotations.Configuration("mapStore.cassandra.clusterName")
  @volatile
  private[cassandra] var clusterName: String = null

  @NotNull
  @annotations.Configuration("mapStore.cassandra.hosts")
  @volatile
  private[cassandra] var hosts: String = null

  @Min(1000L)
  @Max(65535L)
  @annotations.Configuration("mapStore.cassandra.port")
  @volatile
  private[cassandra] var port: Int = ProtocolOptions.DEFAULT_PORT

  override def get(): Initializer = {
    Cluster.builder()
      .withClusterName(clusterName)
      .addContactPoints(hosts.split(","): _*).withPort(port)
      .withInitialListeners(List(this.asInstanceOf[StateListener]).asJavaCollection)
  }

  override def onAdd(host: Host): Unit =
    LOGGER.info(s"Adding Cassandra host: $host")

  override def onUnregister(cluster: Cluster): Unit =
    LOGGER.info(s"Unregistered Cassandra cluster: $cluster")

  override def onRegister(cluster: Cluster): Unit =
    LOGGER.info(s"Registered Cassandra cluster: $cluster")

  override def onRemove(host: Host): Unit =
    LOGGER.info(s"Removed Cassandra host: $host")

  override def onUp(host: Host): Unit =
    LOGGER.info(s"Cassandra host is up: $host")

  override def onDown(host: Host): Unit =
    LOGGER.info(s"Cassandra host is down: $host")
}
