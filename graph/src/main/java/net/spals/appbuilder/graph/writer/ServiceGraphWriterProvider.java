package net.spals.appbuilder.graph.writer;

import com.google.inject.Provider;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.writer.ascii.AsciiServiceGraphWriter;
import org.slf4j.Logger;

/**
 * @author tkral
 */
public class ServiceGraphWriterProvider implements Provider<ServiceGraphWriter> {

    private final String fileName;
    private final Logger logger;
    private final ServiceGraphFormat graphFormat;

    public ServiceGraphWriterProvider(final String fileName,
                                      final Logger logger,
                                      final ServiceGraphFormat graphFormat) {
        this.fileName = fileName;
        this.logger = logger;
        this.graphFormat = graphFormat;
    }


    @Override
    public ServiceGraphWriter get() {
        switch (graphFormat) {
            case ASCII:
                return new AsciiServiceGraphWriter(logger);
            default:
                return new NoOpServiceGraphWriter(logger);
        }
    }

    static class NoOpServiceGraphWriter implements ServiceGraphWriter {

        private final Logger logger;

        NoOpServiceGraphWriter(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public ServiceGraphFormat getFormat() {
            return ServiceGraphFormat.NONE;
        }

        @Override
        public void writeGraph(final ServiceGraph serviceGraph) {
            logger.info("Skipping service graph write...");
        }
    }
}
