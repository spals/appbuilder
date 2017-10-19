package net.spals.appbuilder.graph.writer;

import net.spals.appbuilder.graph.model.IServiceGraphVertex;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * @author tkral
 */
interface ServiceGraphWriterPlugin<V extends IServiceGraphVertex<?>, G extends DirectedGraph<V, DefaultEdge>> {

    String writeServiceGraph(G serviceGraph);
}
