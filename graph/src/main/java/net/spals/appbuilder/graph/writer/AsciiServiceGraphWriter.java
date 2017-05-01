package net.spals.appbuilder.graph.writer;

import com.github.mdr.ascii.graph.Graph;
import com.github.mdr.ascii.java.GraphBuilder;
import com.github.mdr.ascii.java.GraphLayouter;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Stream;

/**
 * @author tkral
 */
class AsciiServiceGraphWriter implements ServiceGraphWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsciiServiceGraphWriter.class);

    private final ServiceGraph serviceGraph;

    AsciiServiceGraphWriter(final ServiceGraph serviceGraph) {
        this.serviceGraph = serviceGraph;
    }

    @Override
    public void writeServiceGraph() {
        final GraphBuilder<AsciiServiceGraphVertex> asciiGraphBuilder = new GraphBuilder<>();
        // 1. Add all the edges to the graph builder
        final Stream<SimpleImmutableEntry<ServiceGraphVertex, ServiceGraphVertex>> singleEdges =
                serviceGraph.getOutboundEdges().entrySet().stream()
                .flatMap(multiEdgeEntry -> {
                    final Stream<SimpleImmutableEntry<ServiceGraphVertex, ServiceGraphVertex>> edges =
                        multiEdgeEntry.getValue().stream()
                                .map(toVertex -> new SimpleImmutableEntry(multiEdgeEntry.getKey(), toVertex));
                    return edges;
                });
        singleEdges.forEach(singleEdgeEntry ->
                asciiGraphBuilder.addEdge(new AsciiServiceGraphVertex(singleEdgeEntry.getKey()), new AsciiServiceGraphVertex(singleEdgeEntry.getValue())));
        // 2. Add all orphaned vertices.
        serviceGraph.getVertices().stream().filter(vertex -> serviceGraph.isOrphan(vertex))
                .forEach(orphanVertex -> asciiGraphBuilder.addVertex(new AsciiServiceGraphVertex(orphanVertex)));

        final Graph<AsciiServiceGraphVertex> asciiGraph = asciiGraphBuilder.build();
        final GraphLayouter<AsciiServiceGraphVertex> asciiGraphLayouter = new GraphLayouter<>();
        asciiGraphLayouter.setVertical(false);

        LOGGER.info(String.format("%n%s", asciiGraphLayouter.layout(asciiGraph)));
    }
}
