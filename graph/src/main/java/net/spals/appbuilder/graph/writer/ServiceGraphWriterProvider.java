package net.spals.appbuilder.graph.writer;

import com.google.inject.Provider;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;

/**
 * @author tkral
 */
public class ServiceGraphWriterProvider implements Provider<ServiceGraphWriter> {

    private final ServiceGraph serviceGraph;
    private final ServiceGraphFormat graphFormat;

    public ServiceGraphWriterProvider(final ServiceGraph serviceGraph,
                                      final ServiceGraphFormat graphFormat) {
        this.serviceGraph = serviceGraph;
        this.graphFormat = graphFormat;
    }


    @Override
    public ServiceGraphWriter get() {
        switch (graphFormat) {
            case ASCII: return new AsciiServiceGraphWriter(serviceGraph);
            default: return new NoOpServiceGraphWriter();
        }
    }
}
