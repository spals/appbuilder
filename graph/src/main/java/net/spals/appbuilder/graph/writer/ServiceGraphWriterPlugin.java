package net.spals.appbuilder.graph.writer;

import net.spals.appbuilder.graph.model.ServiceGraph;

/**
 * @author tkral
 */
interface ServiceGraphWriterPlugin {

    String writeServiceGraph(ServiceGraph serviceGraph);
}
