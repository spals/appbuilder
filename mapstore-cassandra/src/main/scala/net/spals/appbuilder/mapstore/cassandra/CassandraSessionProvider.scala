package net.spals.appbuilder.mapstore.cassandra

import com.datastax.driver.core.Cluster.Initializer
import com.datastax.driver.core.{Cluster, Session}
import com.google.inject.{Inject, Provider}
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindProvider

/**
  * A [[Provider]] of a connected Cassandra session.
  *
  * @author tkral
  */
@AutoBindProvider
private[cassandra] class CassandraSessionProvider @Inject() (@ApplicationName applicationName: String,
                                                             initializer: Initializer)
  extends Provider[Session] {

  override def get(): Session = {
    val cluster = Cluster.buildFrom(initializer)
    cluster.connect(applicationName /*keyspace*/)
  }
}
