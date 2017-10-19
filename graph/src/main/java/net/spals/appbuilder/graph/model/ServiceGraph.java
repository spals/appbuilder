package net.spals.appbuilder.graph.model;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import org.jgrapht.DirectedGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.GraphDelegator;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author tkral
 */
public class ServiceGraph
    extends GraphDelegator<IServiceGraphVertex<?>, DefaultEdge>
    implements DirectedGraph<IServiceGraphVertex<?>, DefaultEdge> {

    public ServiceGraph() {
        super(new DirectedAcyclicGraph<>(DefaultEdge.class));
    }

    public Optional<IServiceGraphVertex<?>> findVertex(final Key<?> guiceKey) {
        return vertexSet().stream()
            .filter(vertex -> guiceKey.equals(vertex.getGuiceKey()))
            .findAny();
    }

    public Set<IServiceGraphVertex<?>> findAllVertices(final Matcher<TypeLiteral<?>> typeMatcher) {
        return vertexSet().stream()
            .filter(vertex -> typeMatcher.matches(vertex.getGuiceKey().getTypeLiteral()))
            .collect(Collectors.toSet());
    }
}
