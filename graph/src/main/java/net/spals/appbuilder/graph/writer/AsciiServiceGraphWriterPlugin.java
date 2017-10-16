package net.spals.appbuilder.graph.writer;

import com.github.mdr.ascii.graph.Graph;
import com.github.mdr.ascii.java.GraphBuilder;
import com.github.mdr.ascii.java.GraphLayouter;
import net.spals.appbuilder.graph.model.IServiceGraphVertex;
import net.spals.appbuilder.graph.model.PrintableVertex;
import net.spals.appbuilder.graph.model.ServiceDAG;

import static net.spals.appbuilder.graph.model.PrintableVertex.createPrintableVertex;

/**
 * @author tkral
 */
class AsciiServiceGraphWriterPlugin implements ServiceGraphWriterPlugin {

    @Override
    public String writeServiceGraph(final ServiceDAG serviceDAG) {
        final GraphBuilder<IServiceGraphVertex> asciiGraphBuilder = new GraphBuilder<>();
        // Add all the edges to the graph builder
        serviceDAG.edgeSet().forEach(edge -> {
            // Use newlines as the separator to make the graph more vertical
            final PrintableVertex<?> edgeSource = createPrintableVertex(
                serviceDAG.getEdgeSource(edge), String.format("%n")
            );
            final PrintableVertex<?> edgeTarget = createPrintableVertex(
                serviceDAG.getEdgeTarget(edge), String.format("%n")
            );
            asciiGraphBuilder.addEdge(edgeSource, edgeTarget);
        });

        final Graph<IServiceGraphVertex> asciiGraph = asciiGraphBuilder.build();
        final GraphLayouter<IServiceGraphVertex> asciiGraphLayouter = new GraphLayouter<>();
        asciiGraphLayouter.setVertical(false);

        return String.format("%n%s", asciiGraphLayouter.layout(asciiGraph));
    }

}
