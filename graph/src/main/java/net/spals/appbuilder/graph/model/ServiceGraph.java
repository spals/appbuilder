package net.spals.appbuilder.graph.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import org.jgrapht.DirectedGraph;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.GraphDelegator;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of a {@link org.jgrapht.DirectedGraph}
 * which stores relationships between micro-services.
 *
 * @author tkral
 */
public class ServiceGraph
    extends GraphDelegator<ServiceGraphVertex<?>, DefaultEdge>
    implements DirectedGraph<ServiceGraphVertex<?>, DefaultEdge> {

    public ServiceGraph() {
        super(new DirectedAcyclicGraph<>(DefaultEdge.class));
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

    public ServiceTree toTree(final ServiceGraphVertex<?> root) {
        final ServiceTree serviceTree = new ServiceTree(root);

        // Reverse the edges of the service graph before we perform our walk.
        // We do this because the service graph is built with the root vertex
        // at the bottom. And this moves it to the top.
        final DirectedGraph<ServiceGraphVertex<?>,DefaultEdge> reversedGraph =
            new EdgeReversedGraph<>(this);
        final BreadthFirstIterator<ServiceGraphVertex<?>, DefaultEdge> bfs =
            new BreadthFirstIterator<>(reversedGraph, root);

        // Walk the graph breadth-first and use the tree conversion listener
        // to convert the DAG into a tree
        final ServiceTreeConversionListener listener = new ServiceTreeConversionListener(serviceTree, reversedGraph);
        bfs.addTraversalListener(listener);
        while (bfs.hasNext()) {
            bfs.next();
        }

        return serviceTree;
    }

    /**
     * A {@link org.jgrapht.event.TraversalListener} which converts a DAG
     * into a tree.
     *
     * Note that this assumes a BFS traversal.
     *
     * @author tkral
     */
    @VisibleForTesting
    static class ServiceTreeConversionListener extends TraversalListenerAdapter<ServiceGraphVertex<?>, DefaultEdge> {

        private final ServiceTree serviceTree;
        private final DirectedGraph<ServiceGraphVertex<?>, DefaultEdge> traversedGraph;

        private final Deque<ServiceTreeVertex<?>> visitedVertices = new LinkedList<>();

        ServiceTreeConversionListener(final ServiceTree serviceTree,
                                      final DirectedGraph<ServiceGraphVertex<?>, DefaultEdge> traversedGraph) {
            this.serviceTree = serviceTree;
            this.traversedGraph = traversedGraph;

            visitedVertices.addLast(serviceTree.getRoot());
        }

        @Override
        public void edgeTraversed(final EdgeTraversalEvent<DefaultEdge> e) {
            final ServiceGraphVertex<?> edgeSource = traversedGraph.getEdgeSource(e.getEdge());
            final ServiceGraphVertex<?> edgeTarget = traversedGraph.getEdgeTarget(e.getEdge());

            while (!visitedVertices.peekFirst().getDelegate().equals(edgeSource)) {
                visitedVertices.removeFirst();
            }

            final ServiceTreeVertex<?> edgeParent = visitedVertices.peekFirst();
            final ServiceTreeVertex<?> edgeChild = ServiceTreeVertex.newChild(edgeTarget, edgeParent);
            visitedVertices.addLast(edgeChild);

            serviceTree.addVertex(edgeChild);
            serviceTree.addEdge(edgeParent, edgeChild);
        }
    }
}
