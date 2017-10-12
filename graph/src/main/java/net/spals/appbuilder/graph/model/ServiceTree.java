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
    extends GraphDelegator<ServiceTreeVertex<?>, DefaultEdge>
    implements DirectedGraph<ServiceTreeVertex<?>, DefaultEdge> {

    private final ServiceTreeVertex<?> root;

    ServiceTree(final ServiceGraphVertex<?> root) {
        this(ServiceTreeVertex.newRoot(root));
    }

    ServiceTree(final ServiceTreeVertex<?> root) {
        super(new DirectedAcyclicGraph<>(DefaultEdge.class));

        Preconditions.checkArgument(root.isRoot());
        this.root = root;
        addVertex(root);
    }

    Optional<ServiceTreeVertex<?>> findVertex(final ServiceGraphVertex<?> delegate,
                                              final ServiceTreeVertex<?> parent) {
        final ServiceTreeVertex<?> vertex = ServiceTreeVertex.newChild(delegate, parent);
        return vertexSet().stream()
            .filter(v -> v.equals(vertex))
            .findAny();
    }

    public ServiceTreeVertex<?> getRoot() {
        return root;
    }

    public Set<ServiceTreeVertex<?>> getSiblings(final ServiceTreeVertex<?> vertex) {
        if (vertex.isRoot()) {
            return Collections.emptySet();
        }

        final ServiceTreeVertex<?> parent = vertex.getParent().get();
        final Set<DefaultEdge> parentEdges = outgoingEdgesOf(parent);

        return parentEdges.stream()
            .map(parentEdge -> getEdgeTarget(parentEdge))
            .filter(sibling -> !sibling.equals(vertex))
            .collect(Collectors.toSet());
    }
}
