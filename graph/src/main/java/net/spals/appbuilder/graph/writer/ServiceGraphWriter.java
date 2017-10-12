package net.spals.appbuilder.graph.writer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.name.Names;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.model.ServiceGraphVertex;
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

    public void writeServiceGraph(final ServiceGraph serviceGraph) {
        removeExternalVertices(serviceGraph, serviceScanMatcher);
        mergeProviderSourceVertices(serviceGraph);
        addApplicationVertex(serviceGraph);

        LOGGER.info(writerPlugin.writeServiceGraph(serviceGraph));
    }

    @VisibleForTesting
    void addApplicationVertex(final ServiceGraph serviceGraph) {
        final ApplicationVertex applicationVertex = new ApplicationVertex(applicationName);
        serviceGraph.addVertex(applicationVertex);

        // Group all micro-services in the graph under the application vertex
        serviceGraph.vertexSet().stream()
            .filter(vertex -> !vertex.equals(applicationVertex))
            // Only group together "top-level" services
            .filter(vertex -> serviceGraph.outDegreeOf(vertex) == 0)
            .forEach(vertex -> serviceGraph.addEdge(vertex, applicationVertex));
    }

    @VisibleForTesting
    void mergeProviderSourceVertices(final ServiceGraph serviceGraph) {
        // 1. Find all vertices within the service graph which are providers.
        final Set<ServiceGraphVertex<?>> providerSourceVertices =
            serviceGraph.findAllVertices(rawTypeThat(subclassesOf(Provider.class)));

        // 2. Search the service graph for the vertices which are provided by the provider vertices
        final Map<ServiceGraphVertex<?>, Optional<ServiceGraphVertex<?>>> providerSourceMap =
            providerSourceVertices.stream().collect(Collectors.toMap(Function.identity(),
                providerSourceVertex -> {
                    final TypeLiteral<?> providerSourceLiteral = providerSourceVertex.getGuiceKey().getTypeLiteral();
                    try {
                        final Method getMethod = providerSourceLiteral.getRawType().getDeclaredMethod("get");
                        final TypeLiteral<?> providedLiteral = providerSourceLiteral.getReturnType(getMethod);
                        return serviceGraph.findVertex(Key.get(providedLiteral));
                    } catch (Throwable t) {
                        return Optional.empty();
                    }
                }));

        // 3. For each provider, provided pair, merge them together within the service graph
        providerSourceMap.entrySet().stream()
            .filter(entry -> entry.getValue().isPresent())
            .forEach(entry -> mergeProviderSourceVertex(serviceGraph, entry.getKey(), entry.getValue().get()));
    }

    @VisibleForTesting
    void mergeProviderSourceVertex(final ServiceGraph serviceGraph,
                                   final ServiceGraphVertex<?> providerSourceVertex,
                                   final ServiceGraphVertex<?> providedVertex) {
        final ServiceGraphVertex<?> mergedVertex = ServiceGraphVertex.vertexWithProvider(providedVertex,
            providerSourceVertex);

        serviceGraph.addVertex(mergedVertex);
        serviceGraph.incomingEdgesOf(providerSourceVertex)
            .forEach(edge -> serviceGraph.addEdge(serviceGraph.getEdgeSource(edge), mergedVertex));
        serviceGraph.incomingEdgesOf(providedVertex)
            .forEach(edge -> serviceGraph.addEdge(serviceGraph.getEdgeSource(edge), mergedVertex));
        serviceGraph.outgoingEdgesOf(providerSourceVertex)
            .forEach(edge -> serviceGraph.addEdge(mergedVertex, serviceGraph.getEdgeTarget(edge)));
        serviceGraph.outgoingEdgesOf(providedVertex)
            .forEach(edge -> serviceGraph.addEdge(mergedVertex, serviceGraph.getEdgeTarget(edge)));

        serviceGraph.removeAllVertices(ImmutableSet.of(providerSourceVertex, providedVertex));
    }

    @VisibleForTesting
    void removeExternalVertices(final ServiceGraph serviceGraph,
                                final Matcher<TypeLiteral<?>> serviceScanMatcher) {
        // Find and remove external vertices. External vertices are defined
        // as services which were not explicitly included in the service scan
        // which are not connected to the rest of the graph.
        final Set<ServiceGraphVertex<?>> externalVertices = serviceGraph.vertexSet().stream()
            .filter(vertex -> !serviceScanMatcher.matches(vertex.getGuiceKey().getTypeLiteral()))
            .filter(vertex -> serviceGraph.outDegreeOf(vertex) == 0 && serviceGraph.inDegreeOf(vertex) == 0)
            .collect(Collectors.toSet());
        serviceGraph.removeAllVertices(externalVertices);
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
        public Optional<ServiceGraphVertex<?>> getProviderSource() {
            return Optional.empty();
        }

        @Override
        protected String toString(final String separator) {
            return "APP[" + applicationName + "]";
        }
    }
}
