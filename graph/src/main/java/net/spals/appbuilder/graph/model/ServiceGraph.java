package net.spals.appbuilder.graph.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Key;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author tkral
 */
public class ServiceGraph {

    private final Multimap<ServiceGraphVertex, ServiceGraphVertex> outboundEdges = HashMultimap.create();
    private final Multimap<ServiceGraphVertex, ServiceGraphVertex> inboundEdges = HashMultimap.create();

    private final Set<ServiceGraphVertex> vertices = new HashSet<>();

    public synchronized ServiceGraph addEdge(final Key<?> fromKey, final Key<?> toKey) {
        return addEdge(new ServiceGraphVertex.Builder().setGuiceKey(fromKey).build(),
                       new ServiceGraphVertex.Builder().setGuiceKey(toKey).build());
    }

    public synchronized ServiceGraph addEdge(final ServiceGraphVertex fromVertex, final ServiceGraphVertex toVertex) {
        outboundEdges.put(fromVertex, toVertex);
        inboundEdges.put(toVertex, fromVertex);
        return this;
    }

    public ServiceGraph addVertex(final Key<?> key) {
        return addVertex(new ServiceGraphVertex.Builder().setGuiceKey(key).build());
    }

    public ServiceGraph addVertex(final ServiceGraphVertex vertex) {
        vertices.add(vertex);
        return this;
    }

    public Map<ServiceGraphVertex, Collection<ServiceGraphVertex>> getInboundEdges() {
        return inboundEdges.asMap();
    }

    public Map<ServiceGraphVertex, Collection<ServiceGraphVertex>> getOutboundEdges() {
        return outboundEdges.asMap();
    }

    public Set<ServiceGraphVertex> getVertices() {
        return vertices;
    }

    public boolean isOrphan(final ServiceGraphVertex vertex) {
        // An orphan is one without any outbound or inbound edges
        return outboundEdges.get(vertex).isEmpty() &&
                inboundEdges.get(vertex).isEmpty();
    }
}
