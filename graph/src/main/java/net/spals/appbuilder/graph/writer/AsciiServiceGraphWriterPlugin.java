package net.spals.appbuilder.graph.writer;

import com.github.mdr.ascii.graph.Graph;
import com.github.mdr.ascii.java.GraphBuilder;
import com.github.mdr.ascii.java.GraphLayouter;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphVertex;

/**
 * @author tkral
 */
class AsciiServiceGraphWriterPlugin implements ServiceGraphWriterPlugin {

    @Override
    public String writeServiceGraph(final ServiceGraph serviceGraph) {
        final GraphBuilder<ServiceGraphVertex> asciiGraphBuilder = new GraphBuilder<>();
        // Add all the edges to the graph builder
        serviceGraph.edgeSet().forEach(edge ->
            asciiGraphBuilder.addEdge(edge.getSourceVertex(), edge.getTargetVertex()));

        final Graph<ServiceGraphVertex> asciiGraph = asciiGraphBuilder.build();
        final GraphLayouter<ServiceGraphVertex> asciiGraphLayouter = new GraphLayouter<>();
        asciiGraphLayouter.setVertical(false);

        return String.format("%n%s", asciiGraphLayouter.layout(asciiGraph));
    }
}
