package net.spals.appbuilder.graph.writer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Key;
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

    private final String applicationName;
    private final Matcher<TypeLiteral<?>> serviceScanMatcher;
    private final ServiceGraphWriterPlugin writerPlugin;

    public ServiceGraphWriter(final String applicationName,
                              final ServiceGraphFormat graphFormat,
                              final ServiceScan serviceScan) {
        this.applicationName = applicationName;
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
        // Find and remove external vertices. External vertices are defined
        // as services which were not explicitly included in the service scan
        // which are not connected to the rest of the graph.
        final Set<ServiceGraphVertex<?>> externalVertices = serviceGraph.vertexSet().stream()
            .filter(vertex -> !serviceScanMatcher.matches(vertex.getGuiceKey().getTypeLiteral()))
            .filter(vertex -> serviceGraph.outDegreeOf(vertex) == 0 && serviceGraph.inDegreeOf(vertex) == 0)
            .collect(Collectors.toSet());
        serviceGraph.removeAllVertices(externalVertices);

        // Group all micro-services in the graph under the application vertex
        final ApplicationVertex applicationVertex = new ApplicationVertex(applicationName);
        serviceGraph.addVertex(applicationVertex);
        serviceGraph.vertexSet().stream()
            .filter(vertex -> !vertex.equals(applicationVertex))
            // Only group together "top-level" services
            .filter(vertex -> serviceGraph.outDegreeOf(vertex) == 0)
            .forEach(vertex -> serviceGraph.addEdge(vertex, applicationVertex));

        LOGGER.info(writerPlugin.writeServiceGraph(serviceGraph));
    }

    /**
     * Special {@link ServiceGraphVertex} instance which
     * represents the application itself.
     *
     * All top-level micro-services will have an outgoing
     * edge to this vertex in order to show a complete graph.
     *
     * @author tkral
     */
    static class ApplicationVertex extends ServiceGraphVertex<String> {

        private final String applicationName;

        public ApplicationVertex(final String applicationName) {
            this.applicationName = applicationName;
        }

        @Override
        public Key<String> getGuiceKey() {
            return Key.get(String.class);
        }

        @Override
        public String getServiceInstance() {
            return applicationName;
        }

        @Override
        public String toString() {
            return "APP[" + applicationName + "]";
        }
    }
}
