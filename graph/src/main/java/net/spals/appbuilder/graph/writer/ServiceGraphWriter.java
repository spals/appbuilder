package net.spals.appbuilder.graph.writer;

import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;

/**
 * @author tkral
 */
public interface ServiceGraphWriter {

    ServiceGraphFormat getFormat();

    void writeGraph(ServiceGraph serviceGraph);
}
