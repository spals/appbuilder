package net.spals.appbuilder.graph.model;

import com.google.common.base.Preconditions;
import org.jgrapht.DirectedGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.GraphDelegator;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of a {@link org.jgrapht.DirectedGraph}
 * which stores relationships between micro-services.
 *
 * @author tkral
 */
public class ServiceTree
    extends GraphDelegator<IServiceTreeVertex<?>, DefaultEdge>
    implements DirectedGraph<IServiceTreeVertex<?>, DefaultEdge> {

    private final IServiceTreeVertex<?> root;

    ServiceTree(final IServiceDAGVertex<?> root) {
        this(ServiceTreeVertex.createRoot(root));
    }

    ServiceTree(final IServiceTreeVertex<?> root) {
        super(new DirectedAcyclicGraph<>(DefaultEdge.class));

        Preconditions.checkArgument(root.isRoot());
        this.root = root;
        addVertex(root);
    }

    Optional<IServiceTreeVertex<?>> findVertex(final IServiceDAGVertex<?> delegate,
                                              final IServiceTreeVertex<?> parent) {
        final IServiceTreeVertex<?> vertex = ServiceTreeVertex.createChild(delegate, parent);
        return vertexSet().stream()
            .filter(v -> v.equals(vertex))
            .findAny();
    }

    public IServiceTreeVertex<?> getRoot() {
        return root;
    }

    public Set<IServiceTreeVertex<?>> getSiblings(final IServiceTreeVertex<?> vertex) {
        if (vertex.isRoot()) {
            return Collections.emptySet();
        }

        final IServiceTreeVertex<?> parent = vertex.getParent().get();
        final Set<DefaultEdge> parentEdges = outgoingEdgesOf(parent);

        return parentEdges.stream()
            .map(parentEdge -> getEdgeTarget(parentEdge))
            .filter(sibling -> !sibling.equals(vertex))
            .collect(Collectors.toSet());
    }
}
