package net.spals.appbuilder.graph.writer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.name.Names;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.ServiceDAG;
import net.spals.appbuilder.graph.model.ServiceDAGVertex;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;

/**
 * @author tkral
 */
public class ServiceGraphWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceGraphWriter.class);

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
            case TEXT: return new TextServiceGraphWriterPlugin();
            default: return new NoOpServiceGraphWriterPlugin();
        }
    }

    public void writeServiceGraph(final ServiceDAG serviceDAG) {
        removeExternalVertices(serviceDAG, serviceScanMatcher);
        mergeProviderSourceVertices(serviceDAG);
        addApplicationVertex(serviceDAG);

        LOGGER.info(writerPlugin.writeServiceGraph(serviceDAG));
    }

    @VisibleForTesting
    void addApplicationVertex(final ServiceDAG serviceDAG) {
        final ApplicationVertex applicationVertex = new ApplicationVertex(applicationName);
        serviceDAG.addVertex(applicationVertex);

        // Group all micro-services in the graph under the application vertex
        serviceDAG.vertexSet().stream()
            .filter(vertex -> !vertex.equals(applicationVertex))
            // Only group together "top-level" services
            .filter(vertex -> serviceDAG.outDegreeOf(vertex) == 0)
            .forEach(vertex -> serviceDAG.addEdge(vertex, applicationVertex));
    }

    @VisibleForTesting
    void mergeProviderSourceVertices(final ServiceDAG serviceDAG) {
        // 1. Find all vertices within the service graph which are providers.
        final Set<ServiceDAGVertex<?>> providerSourceVertices =
            serviceDAG.findAllVertices(rawTypeThat(subclassesOf(Provider.class)));

        // 2. Search the service graph for the vertices which are provided by the provider vertices
        final Map<ServiceDAGVertex<?>, Optional<ServiceDAGVertex<?>>> providerSourceMap =
            providerSourceVertices.stream().collect(Collectors.toMap(Function.identity(),
                providerSourceVertex -> {
                    final TypeLiteral<?> providerSourceLiteral = providerSourceVertex.getGuiceKey().getTypeLiteral();
                    try {
                        final Method getMethod = providerSourceLiteral.getRawType().getDeclaredMethod("get");
                        final TypeLiteral<?> providedLiteral = providerSourceLiteral.getReturnType(getMethod);
                        return serviceDAG.findVertex(Key.get(providedLiteral));
                    } catch (Throwable t) {
                        return Optional.empty();
                    }
                }));

        // 3. For each provider, provided pair, merge them together within the service graph
        providerSourceMap.entrySet().stream()
            .filter(entry -> entry.getValue().isPresent())
            .forEach(entry -> mergeProviderSourceVertex(serviceDAG, entry.getKey(), entry.getValue().get()));
    }

    @VisibleForTesting
    void mergeProviderSourceVertex(final ServiceDAG serviceDAG,
                                   final ServiceDAGVertex<?> providerSourceVertex,
                                   final ServiceDAGVertex<?> providedVertex) {
        final ServiceDAGVertex<?> mergedVertex = ServiceDAGVertex.createVertexWithProvider(providedVertex,
            providerSourceVertex);

        serviceDAG.addVertex(mergedVertex);
        serviceDAG.incomingEdgesOf(providerSourceVertex)
            .forEach(edge -> serviceDAG.addEdge(serviceDAG.getEdgeSource(edge), mergedVertex));
        serviceDAG.incomingEdgesOf(providedVertex)
            .forEach(edge -> serviceDAG.addEdge(serviceDAG.getEdgeSource(edge), mergedVertex));
        serviceDAG.outgoingEdgesOf(providerSourceVertex)
            .forEach(edge -> serviceDAG.addEdge(mergedVertex, serviceDAG.getEdgeTarget(edge)));
        serviceDAG.outgoingEdgesOf(providedVertex)
            .forEach(edge -> serviceDAG.addEdge(mergedVertex, serviceDAG.getEdgeTarget(edge)));

        serviceDAG.removeAllVertices(ImmutableSet.of(providerSourceVertex, providedVertex));
    }

    @VisibleForTesting
    void removeExternalVertices(final ServiceDAG serviceDAG,
                                final Matcher<TypeLiteral<?>> serviceScanMatcher) {
        // Find and remove external vertices. External vertices are defined
        // as services which were not explicitly included in the service scan
        // which are not connected to the rest of the graph.
        final Set<ServiceDAGVertex<?>> externalVertices = serviceDAG.vertexSet().stream()
            .filter(vertex -> !serviceScanMatcher.matches(vertex.getGuiceKey().getTypeLiteral()))
            .filter(vertex -> serviceDAG.outDegreeOf(vertex) == 0 && serviceDAG.inDegreeOf(vertex) == 0)
            .collect(Collectors.toSet());
        serviceDAG.removeAllVertices(externalVertices);
    }

    /**
     * Special {@link ServiceDAGVertex} instance which
     * represents the application itself.
     *
     * All top-level micro-services will have an outgoing
     * edge to this vertex in order to show a complete graph.
     *
     * @author tkral
     */
    static class ApplicationVertex extends ServiceDAGVertex<String> {

        static Key<String> APPLICATION_VERTEX_KEY =
            Key.get(String.class, Names.named(ApplicationVertex.class.getName()));

        private final String applicationName;

        public ApplicationVertex(final String applicationName) {
            this.applicationName = applicationName;
        }

        @Override
        public Key<String> getGuiceKey() {
            return APPLICATION_VERTEX_KEY;
        }

        @Override
        public String getServiceInstance() {
            return applicationName;
        }

        @Override
        public Optional<ServiceDAGVertex<?>> getProviderSource() {
            return Optional.empty();
        }

        @Override
        protected String toString(final String separator) {
            return "APP[" + applicationName + "]";
        }
    }
}
