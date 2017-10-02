package net.spals.appbuilder.graph.writer;

import net.spals.appbuilder.graph.model.ServiceGraph;

/**
 * @author tkral
 */
class NoOpServiceGraphWriterPlugin implements ServiceGraphWriterPlugin {

    @Override
    public String writeServiceGraph(final ServiceGraph serviceGraph) {
        return "Skipping service graph write...";
    }
}
