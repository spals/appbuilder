package net.spals.appbuilder.graph.writer;

import net.spals.appbuilder.graph.model.IServiceTreeVertex;
import net.spals.appbuilder.graph.model.ServiceTree;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author tkral
 */
class TextServiceGraphWriterPlugin implements ServiceGraphWriterPlugin<IServiceTreeVertex<?>, ServiceTree> {

    @Override
    public String writeServiceGraph(final ServiceTree serviceTree) {
        final StringBuilder textGraph = new StringBuilder().append(String.format("%n"));
        final DepthFirstIterator<IServiceTreeVertex<?>, DefaultEdge> dfs =
            new DepthFirstIterator<>(serviceTree, serviceTree.getRoot());

        // Walk the graph depth-first and use the service tree listener
        // to create a text representation of the service tree
        final TextServiceTreeListener listener = new TextServiceTreeListener(serviceTree, textGraph);
        dfs.addTraversalListener(listener);
        while (dfs.hasNext()) {
            dfs.next();
        }

        return textGraph.toString();
    }

    /**
     * A {@link org.jgrapht.event.TraversalListener} which generates a text
     * tree from a tree traversal.
     *
     * This is shamelessly copied from the maven-dependency-plugin:tree target.
     *
     * @author tkral
     */
    static class TextServiceTreeListener extends TraversalListenerAdapter<IServiceTreeVertex<?>, DefaultEdge> {

        private static final String VERTEX_INDENT = "+- ";
        private static final String LAST_VERTEX_INDENT = "\\- ";
        private static final String FILL_INDENT = "|  ";
        private static final String LAST_FILL_INDENT = "   ";

        private final ServiceTree serviceTree;
        private final StringBuilder textGraph;

        private final AtomicInteger graphDepth = new AtomicInteger(0);
        private final Set<IServiceTreeVertex<?>> visitedVertices = new HashSet<>();
        TextServiceTreeListener(final ServiceTree serviceTree,
                                final StringBuilder textGraph) {
            this.serviceTree = serviceTree;
            this.textGraph = textGraph;
        }

        @Override
        public void vertexTraversed(final VertexTraversalEvent<IServiceTreeVertex<?>> e) {
            visitedVertices.add(e.getVertex());

            indent(e.getVertex());
            textGraph.append(e.getVertex()).append(String.format("%n"));

            graphDepth.incrementAndGet();
        }

        @Override
        public void vertexFinished(final VertexTraversalEvent<IServiceTreeVertex<?>> e) {
            graphDepth.decrementAndGet();
        }

        private void indent(final IServiceTreeVertex<?> vertex) {
            for (int i = 1; i < graphDepth.get(); i++) {
                textGraph.append(isLast(vertex, i) ? LAST_FILL_INDENT : FILL_INDENT);
            }

            if (graphDepth.get() > 0) {
                textGraph.append(isLast(vertex) ? LAST_VERTEX_INDENT : VERTEX_INDENT);
            }
        }

        private boolean isLast(final IServiceTreeVertex<?> vertex) {
            if (vertex.isRoot()) {
                return true;
            }

            final Set<IServiceTreeVertex<?>> siblings = serviceTree.getSiblings(vertex);
            return visitedVertices.containsAll(siblings);
        }

        private boolean isLast(final IServiceTreeVertex<?> vertex, final int ancestorDepth) {
            IServiceTreeVertex<?> depthVertex = vertex;
            int distance = graphDepth.get() - ancestorDepth;

            while (distance-- > 0) {
                depthVertex = depthVertex.getParent().get();
            }

            return isLast(depthVertex);
        }
    }
}
