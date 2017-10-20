package net.spals.appbuilder.graph.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.name.Names;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.config.service.ServiceScan;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.annotatedWith;
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
        mergeSingletonVertices(serviceDAG);
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
            .forEach(entry -> {
                final IServiceDAGVertex<?> providerSourceVertex = entry.getKey();
                final IServiceDAGVertex<?> providedVertex = entry.getValue().get();
                final IServiceDAGVertex<?> mergedVertex = createDAGVertexWithProvider(providedVertex,
                    providerSourceVertex);
                mergeVertices(serviceDAG, providerSourceVertex, providedVertex, mergedVertex);
            });
    }

    @VisibleForTesting
    void mergeSingletonVertices(final ServiceDAG serviceDAG) {
        // 1.
        final Map<IServiceDAGVertex<?>, Optional<IServiceDAGVertex<?>>> singletonVertexMap =
            serviceDAG.findAllVertices(annotatedWith(AutoBindSingleton.class)).stream()
                .filter(singletonVertex -> {
                    final Class<?> baseClass = singletonVertex.getServiceInstance().getClass()
                        .getAnnotation(AutoBindSingleton.class).baseClass();
                    return !baseClass.equals(Void.class);
                })
                .collect(Collectors.toMap(Function.identity(),
                    singletonVertex -> {
                        final Class<?> baseClass = singletonVertex.getServiceInstance().getClass()
                            .getAnnotation(AutoBindSingleton.class).baseClass();
                        return serviceDAG.findVertex(Key.get(baseClass));
                    }));

        singletonVertexMap.entrySet().stream()
            .filter(entry -> entry.getValue().isPresent())
            .forEach(entry -> {
                final IServiceDAGVertex<?> classVertex = entry.getKey();
                final IServiceDAGVertex<?> interfaceVertex = entry.getValue().get();
                mergeVertices(serviceDAG, classVertex, interfaceVertex, classVertex);
            });
    }

    @VisibleForTesting
    void mergeVertices(final ServiceDAG serviceDAG,
                       final IServiceDAGVertex<?> vertex1,
                       final IServiceDAGVertex<?> vertex2,
                       final IServiceDAGVertex<?> mergedVertex) {

        final Set<IServiceDAGVertex<?>> incomingEdgeSources = ImmutableSet.<IServiceDAGVertex<?>>builder()
            .addAll(serviceDAG.incomingEdgesOf(vertex1).stream().map(edge -> serviceDAG.getEdgeSource(edge))
                .collect(Collectors.toSet()))
            .addAll(serviceDAG.incomingEdgesOf(vertex2).stream().map(edge -> serviceDAG.getEdgeSource(edge))
                .collect(Collectors.toSet()))
            .build();
        final Set<IServiceDAGVertex<?>> outgoingEdgeTargets = ImmutableSet.<IServiceDAGVertex<?>>builder()
            .addAll(serviceDAG.outgoingEdgesOf(vertex1).stream().map(edge -> serviceDAG.getEdgeTarget(edge))
                .collect(Collectors.toSet()))
            .addAll(serviceDAG.outgoingEdgesOf(vertex2).stream().map(edge -> serviceDAG.getEdgeTarget(edge))
                .collect(Collectors.toSet()))
            .build();

        serviceDAG.removeAllVertices(ImmutableSet.of(vertex1, vertex2));
        serviceDAG.addVertex(mergedVertex);
        incomingEdgeSources.forEach(edgeSource -> serviceDAG.addEdge(edgeSource, mergedVertex));
        outgoingEdgeTargets.forEach(edgeTarget -> serviceDAG.addEdge(mergedVertex, edgeTarget));
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
