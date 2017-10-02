package net.spals.appbuilder.graph.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Key;
import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * An implementation of a {@link org.jgrapht.DirectedGraph}
 * which stores relationships between micro-services.
 *
 * @author tkral
 */
public class ServiceGraph implements DirectedGraph<ServiceGraphVertex<?>, ServiceGraphEdge> {

    private final DirectedGraph<ServiceGraphVertex<?>, ServiceGraphEdge> delegate;

    public ServiceGraph() {
        this(new DirectedAcyclicGraph<>(ServiceGraphEdge.class));
    }

    @VisibleForTesting
    ServiceGraph(DirectedGraph<ServiceGraphVertex<?>, ServiceGraphEdge> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int inDegreeOf(final ServiceGraphVertex<?> vertex) {
        return delegate.inDegreeOf(vertex);
    }

    @Override
    public Set<ServiceGraphEdge> incomingEdgesOf(final ServiceGraphVertex<?> vertex) {
        return delegate.incomingEdgesOf(vertex);
    }

    @Override
    public int outDegreeOf(final ServiceGraphVertex<?> vertex) {
        return delegate.outDegreeOf(vertex);
    }

    @Override
    public Set<ServiceGraphEdge> outgoingEdgesOf(final ServiceGraphVertex<?> vertex) {
        return delegate.outgoingEdgesOf(vertex);
    }

    @Override
    public Set<ServiceGraphEdge> getAllEdges(final ServiceGraphVertex<?> sourceVertex,
                                             final ServiceGraphVertex<?> targetVertex) {
        return delegate.getAllEdges(sourceVertex, targetVertex);
    }

    @Override
    public ServiceGraphEdge getEdge(final ServiceGraphVertex<?> sourceVertex,
                                    final ServiceGraphVertex<?> targetVertex) {
        return delegate.getEdge(sourceVertex, targetVertex);
    }

    @Override
    public EdgeFactory<ServiceGraphVertex<?>, ServiceGraphEdge> getEdgeFactory() {
        return delegate.getEdgeFactory();
    }

    @Override
    public ServiceGraphEdge addEdge(final ServiceGraphVertex<?> sourceVertex,
                                    final ServiceGraphVertex<?> targetVertex) {
        return delegate.addEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean addEdge(final ServiceGraphVertex<?> sourceVertex,
                           final ServiceGraphVertex<?> targetVertex,
                           final ServiceGraphEdge edge) {
        return delegate.addEdge(sourceVertex, targetVertex, edge);
    }

    @Override
    public boolean addVertex(final ServiceGraphVertex<?> vertex) {
        return delegate.addVertex(vertex);
    }

    @Override
    public boolean containsEdge(final ServiceGraphVertex<?> sourceVertex,
                                final ServiceGraphVertex<?> targetVertex) {
        return delegate.containsEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean containsEdge(final ServiceGraphEdge edge) {
        return delegate.containsEdge(edge);
    }

    @Override
    public boolean containsVertex(final ServiceGraphVertex<?> vertex) {
        return delegate.containsVertex(vertex);
    }

    @Override
    public Set<ServiceGraphEdge> edgeSet() {
        return delegate.edgeSet();
    }

    @Override
    public Set<ServiceGraphEdge> edgesOf(final ServiceGraphVertex<?> vertex) {
        return delegate.edgesOf(vertex);
    }

    public Optional<ServiceGraphVertex<?>> findVertex(final Key<?> guiceKey) {
        return vertexSet().stream()
            .filter(vertex -> guiceKey.equals(vertex.getGuiceKey()))
            .findAny();
    }

    @Override
    public boolean removeAllEdges(final Collection<? extends ServiceGraphEdge> edges) {
        return delegate.removeAllEdges(edges);
    }

    @Override
    public Set<ServiceGraphEdge> removeAllEdges(final ServiceGraphVertex<?> sourceVertex,
                                           final ServiceGraphVertex<?> targetVertex) {
        return delegate.removeAllEdges(sourceVertex, targetVertex);
    }

    @Override
    public boolean removeAllVertices(final Collection<? extends ServiceGraphVertex<?>> vertices) {
        return delegate.removeAllVertices(vertices);
    }

    @Override
    public ServiceGraphEdge removeEdge(final ServiceGraphVertex<?> sourceVertex,
                                       final ServiceGraphVertex<?> targetVertex) {
        return delegate.removeEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean removeEdge(final ServiceGraphEdge edge) {
        return delegate.removeEdge(edge);
    }

    @Override
    public boolean removeVertex(final ServiceGraphVertex<?> vertex) {
        return delegate.removeVertex(vertex);
    }

    @Override
    public Set<ServiceGraphVertex<?>> vertexSet() {
        return delegate.vertexSet();
    }

    @Override
    public ServiceGraphVertex<?> getEdgeSource(final ServiceGraphEdge edge) {
        return delegate.getEdgeSource(edge);
    }

    @Override
    public ServiceGraphVertex<?> getEdgeTarget(final ServiceGraphEdge edge) {
        return delegate.getEdgeTarget(edge);
    }

    @Override
    public double getEdgeWeight(final ServiceGraphEdge edge) {
        return delegate.getEdgeWeight(edge);
    }
}
