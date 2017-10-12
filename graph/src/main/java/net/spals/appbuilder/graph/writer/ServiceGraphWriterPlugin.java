package net.spals.appbuilder.graph.writer;

import net.spals.appbuilder.graph.model.ServiceDAG;

/**
 * @author tkral
 */
interface ServiceGraphWriterPlugin {

    String writeServiceGraph(ServiceDAG serviceDAG);
}
