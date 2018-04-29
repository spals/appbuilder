package net.spals.appbuilder.app.grpc;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.AbstractStub;
import net.spals.appbuilder.app.core.jaxrs.JaxRsCorsModule;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.IServiceGraphVertex;
import net.spals.appbuilder.graph.model.ServiceDAGVertex;
import net.spals.appbuilder.graph.model.ServiceGraph;
import org.glassfish.jersey.server.ResourceConfig;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
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
    public abstract String getApplicationName();
    public abstract ServerBuilder<?> getGrpcExternalServerBuilder();
    public abstract ServiceGraph getServiceGraph();
    public abstract ServiceScan getServiceScan();

    public abstract boolean isRestServerEnabled();
    public abstract boolean isCorsEnabled();
    public abstract Optional<InProcessServerBuilder> getGrpcInternalServerBuilder();
    public abstract Optional<ResourceConfig> getRestResourceConfig();

    abstract Map<Class<?>, Constructor<?>> getRestResourceConstructors();

    public static class Builder extends GrpcWebServerModule_Builder {
        public Builder() {
            setWebServerAutoBindingEnabled(true);
            setServiceScan(ServiceScan.empty());

            setRestServerEnabled(false);
            setGrpcInternalServerBuilder(Optional.empty());
            setRestResourceConfig(Optional.empty());
        }

        public Builder enableRestServer(
            final InProcessServerBuilder grpcInternalServerBuilder,
            final ResourceConfig restResourceConfig
        ) {
            setRestServerEnabled(true);
            setGrpcInternalServerBuilder(grpcInternalServerBuilder);
            setRestResourceConfig(restResourceConfig);

            return this;
        }

        @Override
        public GrpcWebServerModule build() {
            if (isRestServerEnabled()) {
                final ServiceScan serviceScan = getServiceScanBuilder().build();
                final Set<Class<?>> restClasses = serviceScan.getReflections().getTypesAnnotatedWith(Path.class);
                restClasses.stream().flatMap(jerseyClass -> {
                    final List<Constructor<?>> ctors = Lists.newArrayList(jerseyClass.getDeclaredConstructors());
                    // Look for rest resources which have a constructor with a single stub parameter.
                    // These are assumed to be gRPC-rest resources.
                    final Optional<Constructor<?>> restCtor = ctors.stream()
                        .filter(ctor -> ctor.getParameterCount() == 1)
                        .filter(ctor -> AbstractStub.class.isAssignableFrom(ctor.getParameterTypes()[0]))
                        .findFirst();

                    return restCtor.map(rCtor -> Collections.singleton(rCtor).stream())
                        .orElseGet(() -> Stream.empty());
                }).forEach(restCtor -> putRestResourceConstructors(restCtor.getParameterTypes()[0], restCtor));

                if (isCorsEnabled() && getRestResourceConfig().isPresent()) {
                    getRestResourceConfig().get().register(JaxRsCorsModule.JaxRsCorsFilter.class);
                }
            }

            return super.build();
        }
    }

    @Override
    protected void configure() {
        final Matcher typeMatcher = rawTypeThat(subclassesOf(BindableService.class))
            .or(rawTypeThat(subclassesOf(ServerServiceDefinition.class)))
            .or(rawTypeThat(subclassesOf(ServerInterceptor.class)))
            .or(rawTypeThat(subclassesOf(ServerTransportFilter.class)))
            .or(rawTypeThat(subclassesOf(HandlerRegistry.class)))
            .or(rawTypeThat(subclassesOf(DecompressorRegistry.class)))
            .or(rawTypeThat(subclassesOf(CompressorRegistry.class)));
        bindListener(typeMatcher, this);
    }

    @Override
    public void afterInjection(final Object wsComponent) {
        LOGGER.info("Registering WebServer component: {}", wsComponent);
        registerGrpcComponent(wsComponent);

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

    Object createGrpcStub(final BindableService grpcService) {
        checkState(isRestServerEnabled());
        checkState(getGrpcInternalServerBuilder().isPresent());

        final Class<?> grpcServiceClass = grpcService.getClass().getSuperclass().getEnclosingClass();
        final Method newStubMethod;
        try {
            newStubMethod = grpcServiceClass.getMethod("newStub", Channel.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find newStub method for gRPC service class: " + grpcServiceClass.getName(), e);
        }

        final Channel internalServerChannel =
            InProcessChannelBuilder.forName(getApplicationName())
                .usePlaintext()
                .directExecutor()
                .build();

        try {
            return newStubMethod.invoke(grpcServiceClass, internalServerChannel);
        } catch (IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException("Unable to create a new stub for gRPC service class: " + grpcServiceClass.getName(), e);
        }
    }

    Object createRestResource(final Object grpcStub) {
        final Constructor<?> restResourceCtor = Optional.ofNullable(getRestResourceConstructors().get(grpcStub.getClass()))
            .orElseThrow(() -> new RuntimeException("No RESTful resource found on classpath for gRPC stub: " + grpcStub.getClass().getName() + "." +
                " This means that you haven't run the grpc-jersey protoc plugin OR you haven't annotated the RPC protobuf" +
                " correctly OR you need to create a Jersey resource class by hand (if you aren't using the grpc-jersey plugin."));
        restResourceCtor.setAccessible(true);

        try {
            return restResourceCtor.newInstance(grpcStub);
        } catch (IllegalAccessException|InstantiationException|InvocationTargetException e) {
            throw new RuntimeException("Unable to instantiate Jersey Rest resource: " + restResourceCtor.getName());
        }
    }

    void registerGrpcComponent(final Object wsComponent) {
        if (wsComponent instanceof BindableService) {
            final BindableService grpcService = (BindableService) wsComponent;
            getGrpcExternalServerBuilder().addService(grpcService);
            if (isRestServerEnabled()) {
                registerRestService(grpcService);
            }
        } else if (wsComponent instanceof ServerInterceptor) {
            final ServerInterceptor grpcInterceptor = (ServerInterceptor) wsComponent;
            getGrpcExternalServerBuilder().intercept(grpcInterceptor);
            if (isRestServerEnabled()) {
                registerRestInterceptor(grpcInterceptor);
            }
        } else if (wsComponent instanceof ServerServiceDefinition) {
            getGrpcExternalServerBuilder().addService((ServerServiceDefinition) wsComponent);
        } else if (wsComponent instanceof ServerTransportFilter) {
            getGrpcExternalServerBuilder().addTransportFilter((ServerTransportFilter) wsComponent);
        } else if (wsComponent instanceof HandlerRegistry) {
            getGrpcExternalServerBuilder().fallbackHandlerRegistry((HandlerRegistry) wsComponent);
        } else if (wsComponent instanceof DecompressorRegistry) {
            getGrpcExternalServerBuilder().decompressorRegistry((DecompressorRegistry) wsComponent);
        } else if (wsComponent instanceof CompressorRegistry) {
            getGrpcExternalServerBuilder().compressorRegistry((CompressorRegistry) wsComponent);
        } else {
            throw new UnsupportedOperationException("Error exists in AppBuilder framework. " +
                "Encountered web server of type " + wsComponent.getClass().getName() + " for auto-binding, " +
                "but do not know how to bind this type within Grpc.");
        }
    }

    void registerRestInterceptor(final ServerInterceptor grpcInterceptor) {
        checkState(isRestServerEnabled());
        checkState(getGrpcInternalServerBuilder().isPresent());

        getGrpcInternalServerBuilder().get().intercept(grpcInterceptor);
    }

    void registerRestService(final BindableService grpcService) {
        checkState(isRestServerEnabled());
        checkState(getGrpcInternalServerBuilder().isPresent());
        checkState(getRestResourceConfig().isPresent());

        getGrpcInternalServerBuilder().get().addService(grpcService);

        final Object grpcStub = createGrpcStub(grpcService);
        final Object restResource = createRestResource(grpcStub);

        getRestResourceConfig().get().register(restResource);
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
