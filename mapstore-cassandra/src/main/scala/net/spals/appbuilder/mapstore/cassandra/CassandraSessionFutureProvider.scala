package net.spals.appbuilder.mapstore.cassandra

import java.util.concurrent.{Callable, FutureTask}

import com.datastax.driver.core.Cluster.Initializer
import com.datastax.driver.core.{Cluster, Session}
import com.google.common.util.concurrent.ListenableFutureTask
import com.google.inject.{Inject, Provider}
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindProvider

/**
  * A [[Provider]] of a [[FutureTask]] for
  * a connected Cassandra session.
  *
  * @author tkral
  */
@AutoBindProvider
private[cassandra] class CassandraSessionFutureProvider @Inject()(
    @ApplicationName applicationName: String,
    initializer: Initializer
  ) extends Provider[FutureTask[Session]] {

  override def get(): FutureTask[Session] = {
    val cluster = Cluster.buildFrom(initializer)
    // Wrap the cluster connection in a FutureTask so as to delay
    // the connection until we actually need it. This is useful
    // for testing in that we can load the Cassandra plugin with
    // test values and check for the load, but never actually use
    // the plugin (and therefore, never actually connect to the
    // "cluster").
    ListenableFutureTask.create[Session](new Callable[Session] {
      override def call(): Session = cluster.connect(applicationName /*keyspace*/)
    })
  }
}
