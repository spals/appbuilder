package net.spals.appbuilder.graph.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of a {@link org.jgrapht.DirectedGraph}
 * which stores relationships between micro-services.
 *
 * @author tkral
 */
public class ServiceGraph implements DirectedGraph<ServiceGraphVertex<?>, DefaultEdge> {

    private final DirectedGraph<ServiceGraphVertex<?>, DefaultEdge> delegate;

    public ServiceGraph() {
        this(new DirectedAcyclicGraph<>(DefaultEdge.class));
    }

    @VisibleForTesting
    ServiceGraph(DirectedGraph<ServiceGraphVertex<?>, DefaultEdge> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int inDegreeOf(final ServiceGraphVertex<?> vertex) {
        return delegate.inDegreeOf(vertex);
    }

    @Override
    public Set<DefaultEdge> incomingEdgesOf(final ServiceGraphVertex<?> vertex) {
        return delegate.incomingEdgesOf(vertex);
    }

    @Override
    public int outDegreeOf(final ServiceGraphVertex<?> vertex) {
        return delegate.outDegreeOf(vertex);
    }

    @Override
    public Set<DefaultEdge> outgoingEdgesOf(final ServiceGraphVertex<?> vertex) {
        return delegate.outgoingEdgesOf(vertex);
    }

    @Override
    public Set<DefaultEdge> getAllEdges(final ServiceGraphVertex<?> sourceVertex,
                                        final ServiceGraphVertex<?> targetVertex) {
        return delegate.getAllEdges(sourceVertex, targetVertex);
    }

    @Override
    public DefaultEdge getEdge(final ServiceGraphVertex<?> sourceVertex,
                               final ServiceGraphVertex<?> targetVertex) {
        return delegate.getEdge(sourceVertex, targetVertex);
    }

    @Override
    public EdgeFactory<ServiceGraphVertex<?>, DefaultEdge> getEdgeFactory() {
        return delegate.getEdgeFactory();
    }

    @Override
    public DefaultEdge addEdge(final ServiceGraphVertex<?> sourceVertex,
                               final ServiceGraphVertex<?> targetVertex) {
        return delegate.addEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean addEdge(final ServiceGraphVertex<?> sourceVertex,
                           final ServiceGraphVertex<?> targetVertex,
                           final DefaultEdge edge) {
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
    public boolean containsEdge(final DefaultEdge edge) {
        return delegate.containsEdge(edge);
    }

    @Override
    public boolean containsVertex(final ServiceGraphVertex<?> vertex) {
        return delegate.containsVertex(vertex);
    }

    @Override
    public Set<DefaultEdge> edgeSet() {
        return delegate.edgeSet();
    }

    @Override
    public Set<DefaultEdge> edgesOf(final ServiceGraphVertex<?> vertex) {
        return delegate.edgesOf(vertex);
    }

    public Optional<ServiceGraphVertex<?>> findVertex(final Key<?> guiceKey) {
        return vertexSet().stream()
            .filter(vertex -> guiceKey.equals(vertex.getGuiceKey()))
            .findAny();
    }

    public Set<ServiceGraphVertex<?>> findAllVertices(final Matcher<TypeLiteral<?>> typeMatcher) {
        return vertexSet().stream()
            .filter(vertex -> typeMatcher.matches(vertex.getGuiceKey().getTypeLiteral()))
            .collect(Collectors.toSet());
    }

    @Override
    public boolean removeAllEdges(final Collection<? extends DefaultEdge> edges) {
        return delegate.removeAllEdges(edges);
    }

    @Override
    public Set<DefaultEdge> removeAllEdges(final ServiceGraphVertex<?> sourceVertex,
                                           final ServiceGraphVertex<?> targetVertex) {
        return delegate.removeAllEdges(sourceVertex, targetVertex);
    }

    @Override
    public boolean removeAllVertices(final Collection<? extends ServiceGraphVertex<?>> vertices) {
        return delegate.removeAllVertices(vertices);
    }

    @Override
    public DefaultEdge removeEdge(final ServiceGraphVertex<?> sourceVertex,
                                  final ServiceGraphVertex<?> targetVertex) {
        return delegate.removeEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean removeEdge(final DefaultEdge edge) {
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
    public ServiceGraphVertex<?> getEdgeSource(final DefaultEdge edge) {
        return delegate.getEdgeSource(edge);
    }

    @Override
    public ServiceGraphVertex<?> getEdgeTarget(final DefaultEdge edge) {
        return delegate.getEdgeTarget(edge);
    }

    @Override
    public double getEdgeWeight(final DefaultEdge edge) {
        return delegate.getEdgeWeight(edge);
    }
}
