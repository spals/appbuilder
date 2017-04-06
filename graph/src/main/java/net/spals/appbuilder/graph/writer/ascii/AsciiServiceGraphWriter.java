package net.spals.appbuilder.graph.writer.ascii;

import com.github.mdr.ascii.graph.Graph;
import com.github.mdr.ascii.java.GraphBuilder;
import com.github.mdr.ascii.java.GraphLayouter;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.model.ServiceGraphVertex;
import net.spals.appbuilder.graph.writer.ServiceGraphWriter;
import org.slf4j.Logger;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Stream;

/**
 * @author tkral
 */
public class AsciiServiceGraphWriter implements ServiceGraphWriter {

    private final Logger logger;

    public AsciiServiceGraphWriter(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public ServiceGraphFormat getFormat() {
        return ServiceGraphFormat.ASCII;
    }

    @Override
    public void writeGraph(final ServiceGraph serviceGraph) {
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

        logger.info(String.format("%n%s", asciiGraphLayouter.layout(asciiGraph)));
    }
}
