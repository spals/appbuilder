package net.spals.appbuilder.graph.writer;

import net.spals.appbuilder.graph.model.ServiceDAG;

/**
 * @author tkral
 */
class NoOpServiceGraphWriterPlugin implements ServiceGraphWriterPlugin {

    @Override
    public String writeServiceGraph(final ServiceDAG serviceDAG) {
        return "Skipping service graph write...";
    }
}
