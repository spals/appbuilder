package net.spals.appbuilder.graph.writer;

import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.model.ServiceGraphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

/**
 * @author tkral
 */
public class ServiceGraphWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceGraphWriter.class);

    private final ServiceGraphFormat graphFormat;
    private final ServiceGraphs serviceGraphs;

    public ServiceGraphWriter(final ServiceGraphFormat graphFormat,
                              final ServiceGraphs serviceGraphs) {
        this.graphFormat = graphFormat;
        this.serviceGraphs = serviceGraphs;
    }

    public void writeServiceGraph(final Writer writer) {
        final String serviceGraphStr;
        switch (graphFormat) {
            case ASCII:
                serviceGraphStr = new AsciiServiceGraphWriterPlugin().writeServiceGraph(serviceGraphs.getServiceDAG());
                break;
            case TEXT:
                serviceGraphStr = new TextServiceGraphWriterPlugin().writeServiceGraph(serviceGraphs.getServiceTree());
                break;
            default:
                serviceGraphStr = new NoOpServiceGraphWriterPlugin().writeServiceGraph(serviceGraphs.getServiceGraph());
                break;
        }

        try {
            writer.write(serviceGraphStr);
        } catch (IOException e) {
            LOGGER.warn("Unable to write service graph!!", e);
        }
    }
}
