package net.spals.appbuilder.app.grpc;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.grpc.*;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import net.spals.appbuilder.graph.model.IServiceGraphVertex;
import net.spals.appbuilder.graph.model.ServiceDAGVertex;
import net.spals.appbuilder.graph.model.ServiceGraph;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;
import static net.spals.appbuilder.graph.model.ServiceGraphVertex.createGraphVertex;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class GrpcWebServerModule extends AbstractModule implements InjectionListener<Object>, TypeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcWebServerModule.class);

    public abstract boolean isWebServerAutoBindingEnabled();
    public abstract ServerBuilder<?> getServerBuilder();
    public abstract ServiceGraph getServiceGraph();

    public static class Builder extends GrpcWebServerModule_Builder {
        public Builder() {
            setWebServerAutoBindingEnabled(true);
        }
    }

    @Override
    protected void configure() {
        final Matcher typeMatcher = rawTypeThat(subclassesOf(ServerServiceDefinition.class))
            .or(rawTypeThat(subclassesOf(ServerInterceptor.class)))
            .or(rawTypeThat(subclassesOf(ServerTransportFilter.class)))
            .or(rawTypeThat(subclassesOf(HandlerRegistry.class)))
            .or(rawTypeThat(subclassesOf(DecompressorRegistry.class)))
            .or(rawTypeThat(subclassesOf(CompressorRegistry.class)))
            .or(rawTypeThat(subclassesOf(ExecutorServiceFactory.class)));
        bindListener(typeMatcher, this);
    }

    @Override
    public void afterInjection(final Object wsComponent) {
        LOGGER.info("Registering WebServer component: {}", wsComponent);
        registerComponent(wsComponent);

        final Key<Object> wsKey = (Key<Object>) Key.get(TypeLiteral.get(wsComponent.getClass()));
        final IServiceGraphVertex<?> wsVertex = createGraphVertex(wsKey, wsComponent);
        getServiceGraph().addVertex(wsVertex);
        getServiceGraph().addEdge(wsVertex, theWebServerVertex);
    }

    @Override
    public <I> void hear(
        final TypeLiteral<I> typeLiteral,
        final TypeEncounter<I> typeEncounter
    ) {
        if (isWebServerAutoBindingEnabled()) {
            // Add a dummy GRPC WEBSERVER vertex to the service graph to show how WebServer components
            // relate to one another
            if (!getServiceGraph().containsVertex(theWebServerVertex)) {
                getServiceGraph().addVertex(theWebServerVertex);
            }

            typeEncounter.register(this);
        }
    }

    void registerComponent(final Object wsComponent) {
        if (wsComponent instanceof ServerServiceDefinition) {
            getServerBuilder().addService((ServerServiceDefinition) wsComponent);
        } else if (wsComponent instanceof ServerInterceptor) {
            getServerBuilder().intercept((ServerInterceptor) wsComponent);
        } else if (wsComponent instanceof ServerTransportFilter) {
            getServerBuilder().addTransportFilter((ServerTransportFilter) wsComponent);
        } else if (wsComponent instanceof HandlerRegistry) {
            getServerBuilder().fallbackHandlerRegistry((HandlerRegistry) wsComponent);
        } else if (wsComponent instanceof DecompressorRegistry) {
            getServerBuilder().decompressorRegistry((DecompressorRegistry) wsComponent);
        } else if (wsComponent instanceof CompressorRegistry) {
            getServerBuilder().compressorRegistry((CompressorRegistry) wsComponent);
        } else if (wsComponent instanceof ExecutorServiceFactory) {
            final ExecutorServiceFactory.Key grpcExecutorKey =
                new ExecutorServiceFactory.Key.Builder(getServerBuilder().getClass()).build();
            final Executor grpcExecutor = ((ExecutorServiceFactory) wsComponent).createCachedThreadPool(grpcExecutorKey);
            // Automatically register a managed cached thread pool
            getServerBuilder().executor(grpcExecutor);
        } else {
            throw new UnsupportedOperationException("Error exists in AppBuilder framework. " +
                "Encountered web server of type " + wsComponent.getClass().getName() + " for auto-binding, " +
                "but do not know how to bind this type within Grpc.");
        }
    }

    private static GrpcWebServerVertex theWebServerVertex = new GrpcWebServerVertex();

    /**
     * Special {@link ServiceDAGVertex} instance which
     * represents a Grpc web server.
     *
     * All auto-bound webserver components will have an outgoing
     * edge to this vertex in order to show a complete graph.
     *
     * @author tkral
     */
    static class GrpcWebServerVertex implements IServiceGraphVertex<String> {

        @Override
        public Key<String> getGuiceKey() {
            return Key.get(String.class);
        }

        @Override
        public String getServiceInstance() {
            return "GRPC WEBSERVER";
        }

        @Override
        public String toString(final String separator) {
            return getServiceInstance();
        }
    }
}
