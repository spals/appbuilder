package net.spals.appbuilder.mapstore.cassandra

import java.net.InetSocketAddress

import com.datastax.driver.core.Cluster.Initializer
import com.datastax.driver.core.Host.StateListener
import com.datastax.driver.core.{Cluster, Configuration, Host}
import com.google.inject.Inject
import com.typesafe.config.Config
import net.spals.appbuilder.annotations.config.ServiceConfig
import net.spals.appbuilder.annotations.service.AutoBindSingleton
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Cassandra cluster initializer which reads
  * from the appbuilder service configuration.
  *
  * @author tkral
  */
@AutoBindSingleton(baseClass = classOf[Initializer])
private[cassandra] class CassandraClusterInitializer @Inject() (@ServiceConfig config: Config)
  extends Initializer with StateListener {

  private val LOGGER = LoggerFactory.getLogger(classOf[CassandraClusterInitializer])

  override def getInitialListeners: java.util.Collection[StateListener] =
    List(this.asInstanceOf[StateListener]).asJavaCollection

  override def getClusterName: String =
    config.getString("cassandra.mapStore.clusterName")

  override def getContactPoints: java.util.List[InetSocketAddress] = {
    val port = config.getInt("cassandra.mapStore.port")
    config.getString("cassandra.mapStore.hosts").split(",")
      .map(InetSocketAddress.createUnresolved(_, port)).toList.asJava
  }

  override def getConfiguration: Configuration = Configuration.builder().build()

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
