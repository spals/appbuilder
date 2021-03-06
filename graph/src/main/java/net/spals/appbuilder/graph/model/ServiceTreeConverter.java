package net.spals.appbuilder.graph.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jgrapht.DirectedGraph;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.Collection;

import static net.spals.appbuilder.graph.model.ServiceDAGConverter.ApplicationVertex.APPLICATION_VERTEX_KEY;
import static net.spals.appbuilder.graph.model.ServiceTreeVertex.createTreeChild;

/**
 * @author tkral
 */
class ServiceTreeConverter {

    ServiceTree convertFrom(final ServiceDAG serviceDAG) {
        final IServiceDAGVertex<?> applicationVertex = serviceDAG.findVertex(APPLICATION_VERTEX_KEY).get();
        final ServiceTree serviceTree = new ServiceTree(applicationVertex);

        // Reverse the edges of the service graph before we perform our walk.
        // We do this because the service graph is built with the root vertex
        // at the bottom. And this moves it to the top.
        final DirectedGraph<IServiceDAGVertex<?>,DefaultEdge> reversedGraph =
            new EdgeReversedGraph<>(serviceDAG);
        final TopologicalOrderIterator<IServiceDAGVertex<?>, DefaultEdge> topoOrder =
            new TopologicalOrderIterator<>(reversedGraph);

        // Walk the graph in topographical order and use the tree conversion listener
        // to convert the DAG into a tree
        final ServiceTreeConversionListener listener = new ServiceTreeConversionListener(applicationVertex,
            serviceTree, reversedGraph);
        topoOrder.addTraversalListener(listener);
        while (topoOrder.hasNext()) {
            topoOrder.next();
        }

        return serviceTree;
    }

    /**
     * A {@link org.jgrapht.event.TraversalListener} which converts a DAG
     * into a tree.
     *
     * Note that this assumes a topographical order traversal.
     *
     * @author tkral
     */
    @VisibleForTesting
    static class ServiceTreeConversionListener extends TraversalListenerAdapter<IServiceDAGVertex<?>, DefaultEdge> {

        private final ServiceTree serviceTree;
        private final DirectedGraph<IServiceDAGVertex<?>, DefaultEdge> traversedGraph;

        private final Multimap<IServiceDAGVertex<?>, IServiceTreeVertex<?>> visitedVertices = HashMultimap.create();

        ServiceTreeConversionListener(final IServiceDAGVertex<?> dagRoot,
                                      final ServiceTree serviceTree,
                                      final DirectedGraph<IServiceDAGVertex<?>, DefaultEdge> traversedGraph) {
            this.serviceTree = serviceTree;
            this.traversedGraph = traversedGraph;

            visitedVertices.put(dagRoot, serviceTree.getRoot());
        }

        @Override
        public void edgeTraversed(final EdgeTraversalEvent<DefaultEdge> e) {
            final IServiceDAGVertex<?> dagSource = traversedGraph.getEdgeSource(e.getEdge());
            final IServiceDAGVertex<?> dagTarget = traversedGraph.getEdgeTarget(e.getEdge());

            final Collection<IServiceTreeVertex<?>> treeParents = findVisitedDAGVertices(dagSource);
            treeParents.forEach(treeParent -> addTreeVertex(treeParent, dagTarget));
        }

        private void addTreeVertex(final IServiceTreeVertex<?> treeParent, final IServiceDAGVertex<?> dagChild) {
            final IServiceTreeVertex<?> treeChild = createTreeChild(dagChild, treeParent);
            visitedVertices.put(dagChild, treeChild);

            serviceTree.addVertex(treeChild);
            serviceTree.addEdge(treeParent, treeChild);
        }

        private Collection<IServiceTreeVertex<?>> findVisitedDAGVertices(final IServiceDAGVertex<?> dagVertex) {
            return visitedVertices.get(dagVertex);
        }
    }
}
