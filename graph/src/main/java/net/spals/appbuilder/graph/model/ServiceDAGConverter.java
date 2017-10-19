package net.spals.appbuilder.graph.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.name.Names;
import net.spals.appbuilder.config.service.ServiceScan;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createDAGVertex;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createDAGVertexWithProvider;

/**
 * @author tkral
 */
class ServiceDAGConverter {

    private final String applicationName;
    private final Matcher<TypeLiteral<?>> serviceScanMatcher;

    ServiceDAGConverter(final String applicationName,
                        final ServiceScan serviceScan) {
        this.applicationName = applicationName;
        this.serviceScanMatcher = serviceScan.asTypeLiteralMatcher();
    }

    ServiceDAG convertFrom(final ServiceGraph serviceGraph) {
        final ServiceDAG serviceDAG = new ServiceDAG();
        serviceGraph.vertexSet().forEach(vertex -> serviceDAG.addVertex(createDAGVertex(vertex)));
        serviceGraph.edgeSet().forEach(edge -> serviceDAG.addEdge(createDAGVertex(serviceGraph.getEdgeSource(edge)),
            createDAGVertex(serviceGraph.getEdgeTarget(edge))));

        removeExternalVertices(serviceDAG, serviceScanMatcher);
        mergeProviderSourceVertices(serviceDAG);
        addApplicationVertex(serviceDAG);

        return serviceDAG;
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
        final Set<IServiceDAGVertex<?>> providerSourceVertices =
            serviceDAG.findAllVertices(rawTypeThat(subclassesOf(Provider.class)));

        // 2. Search the service graph for the vertices which are provided by the provider vertices
        final Map<IServiceDAGVertex<?>, Optional<IServiceDAGVertex<?>>> providerSourceMap =
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
                                   final IServiceDAGVertex<?> providerSourceVertex,
                                   final IServiceDAGVertex<?> providedVertex) {
        final IServiceDAGVertex<?> mergedVertex = createDAGVertexWithProvider(providedVertex,
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
        final Set<IServiceDAGVertex<?>> externalVertices = serviceDAG.vertexSet().stream()
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
    static class ApplicationVertex implements IServiceDAGVertex<String> {

        static Key<String> APPLICATION_VERTEX_KEY =
            Key.get(String.class, Names.named(ApplicationVertex.class.getName()));

        private final String applicationName;

        ApplicationVertex(final String applicationName) {
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
        public Optional<IServiceDAGVertex<?>> getProviderSource() {
            return Optional.empty();
        }

        @Override
        public String toString(final String separator) {
            return "APP[" + applicationName + "]";
        }
    }
}
