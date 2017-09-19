package net.spals.appbuilder.mapstore.cassandra

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Cluster.Initializer
import com.google.inject.{Inject, Provider}
import io.opentracing.Tracer
import io.opentracing.contrib.cassandra.TracingCluster
import net.spals.appbuilder.annotations.service.AutoBindProvider

/**
  * A [[Provider]] of a Cassandra [[Cluster]] instance.
  *
  * @author tkral
  */
@AutoBindProvider
private[cassandra] class CassandraClusterProvider @Inject()(
  initializer: Initializer,
  tracer: Tracer
) extends Provider[Cluster] {

  override def get(): Cluster = {
    new TracingCluster(initializer, tracer)
  }
}
