package net.spals.appbuilder.graph.writer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.model.ServiceGraphVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author tkral
 */
public class ServiceGraphWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpServiceGraphWriterPlugin.class);

    private final Matcher<TypeLiteral<?>> serviceScanMatcher;
    private final ServiceGraphWriterPlugin writerPlugin;

    public ServiceGraphWriter(final ServiceGraphFormat graphFormat,
                              final ServiceScan serviceScan) {
        this.serviceScanMatcher = serviceScan.asTypeLiteralMatcher();
        this.writerPlugin = loadWriterPlugin(graphFormat);
    }

    @VisibleForTesting
    ServiceGraphWriterPlugin loadWriterPlugin(final ServiceGraphFormat graphFormat) {
        switch (graphFormat) {
            case ASCII: return new AsciiServiceGraphWriterPlugin();
            default: return new NoOpServiceGraphWriterPlugin();
        }
    }

    public void writeServiceGraph(final ServiceGraph serviceGraph) {
        // Probably more work to do here.
        final Set<ServiceGraphVertex<?>> externalVertices = serviceGraph.vertexSet().stream()
            .filter(vertex -> !serviceScanMatcher.matches(vertex.getGuiceKey().getTypeLiteral()))
            .collect(Collectors.toSet());
        serviceGraph.removeAllVertices(externalVertices);

        LOGGER.info(writerPlugin.writeServiceGraph(serviceGraph));
    }
}
