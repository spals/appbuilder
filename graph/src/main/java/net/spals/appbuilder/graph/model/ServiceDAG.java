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

import java.util.*;
import java.util.stream.Collectors;

/**
 * An implementation of a Directed Acyclic Graph
 * which stores relationships between micro-services.
 *
 * @author tkral
 */
public class ServiceDAG
    extends GraphDelegator<ServiceDAGVertex<?>, DefaultEdge>
    implements DirectedGraph<ServiceDAGVertex<?>, DefaultEdge> {

    public ServiceDAG() {
        super(new DirectedAcyclicGraph<>(DefaultEdge.class));
    }

    public Optional<ServiceDAGVertex<?>> findVertex(final Key<?> guiceKey) {
        return vertexSet().stream()
            .filter(vertex -> guiceKey.equals(vertex.getGuiceKey()))
            .findAny();
    }

    public Set<ServiceDAGVertex<?>> findAllVertices(final Matcher<TypeLiteral<?>> typeMatcher) {
        return vertexSet().stream()
            .filter(vertex -> typeMatcher.matches(vertex.getGuiceKey().getTypeLiteral()))
            .collect(Collectors.toSet());
    }

    public ServiceTree toTree(final ServiceDAGVertex<?> root) {
        final ServiceTree serviceTree = new ServiceTree(root);

        // Reverse the edges of the service graph before we perform our walk.
        // We do this because the service graph is built with the root vertex
        // at the bottom. And this moves it to the top.
        final DirectedGraph<ServiceDAGVertex<?>,DefaultEdge> reversedGraph =
            new EdgeReversedGraph<>(this);
        final BreadthFirstIterator<ServiceDAGVertex<?>, DefaultEdge> bfs =
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
    static class ServiceTreeConversionListener extends TraversalListenerAdapter<ServiceDAGVertex<?>, DefaultEdge> {

        private final ServiceTree serviceTree;
        private final DirectedGraph<ServiceDAGVertex<?>, DefaultEdge> traversedGraph;

        private final Deque<ServiceTreeVertex<?>> visitedVertices = new LinkedList<>();

        ServiceTreeConversionListener(final ServiceTree serviceTree,
                                      final DirectedGraph<ServiceDAGVertex<?>, DefaultEdge> traversedGraph) {
            this.serviceTree = serviceTree;
            this.traversedGraph = traversedGraph;

            visitedVertices.addLast(serviceTree.getRoot());
        }

        @Override
        public void edgeTraversed(final EdgeTraversalEvent<DefaultEdge> e) {
            final ServiceDAGVertex<?> dagSource = traversedGraph.getEdgeSource(e.getEdge());
            final ServiceDAGVertex<?> dagTarget = traversedGraph.getEdgeTarget(e.getEdge());

            final Set<ServiceTreeVertex<?>> treeParents = findVisitedDAGVertices(dagSource);
            treeParents.forEach(treeParent -> addTreeVertex(treeParent, dagTarget));
        }

        private void addTreeVertex(final ServiceTreeVertex<?> treeParent, final ServiceDAGVertex<?> dagChild) {
            final ServiceTreeVertex<?> treeChild = ServiceTreeVertex.createChild(dagChild, treeParent);
            visitedVertices.addLast(treeChild);

            serviceTree.addVertex(treeChild);
            serviceTree.addEdge(treeParent, treeChild);
        }

        private Set<ServiceTreeVertex<?>> findVisitedDAGVertices(final ServiceDAGVertex<?> dagVertex) {
            return visitedVertices.stream()
                .filter(visitedVertex -> visitedVertex.getDelegate().equals(dagVertex))
                .collect(Collectors.toSet());
        }
    }
}
