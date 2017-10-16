package net.spals.appbuilder.graph.writer;

import com.github.mdr.ascii.graph.Graph;
import com.github.mdr.ascii.java.GraphBuilder;
import com.github.mdr.ascii.java.GraphLayouter;
import net.spals.appbuilder.graph.model.PrintableVertex;
import net.spals.appbuilder.graph.model.ServiceDAG;
import net.spals.appbuilder.graph.model.ServiceDAGVertex;

/**
 * @author tkral
 */
class AsciiServiceGraphWriterPlugin implements ServiceGraphWriterPlugin {

    @Override
    public String writeServiceGraph(final ServiceDAG serviceDAG) {
        final GraphBuilder<ServiceDAGVertex> asciiGraphBuilder = new GraphBuilder<>();
        // Add all the edges to the graph builder
        serviceDAG.edgeSet().forEach(edge -> {
            // Use newlines as the separator to make the graph more vertical
            final PrintableVertex<?> edgeSource = new PrintableVertex<>(
                serviceDAG.getEdgeSource(edge), String.format("%n")
            );
            final PrintableVertex<?> edgeTarget = new PrintableVertex<>(
                serviceDAG.getEdgeTarget(edge), String.format("%n")
            );
            asciiGraphBuilder.addEdge(edgeSource, edgeTarget);
        });

        final Graph<ServiceDAGVertex> asciiGraph = asciiGraphBuilder.build();
        final GraphLayouter<ServiceDAGVertex> asciiGraphLayouter = new GraphLayouter<>();
        asciiGraphLayouter.setVertical(false);

        return String.format("%n%s", asciiGraphLayouter.layout(asciiGraph));
    }

}
