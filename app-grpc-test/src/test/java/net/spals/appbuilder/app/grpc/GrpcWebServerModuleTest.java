package net.spals.appbuilder.app.grpc;

import com.google.inject.Guice;
import com.google.inject.Module;
import io.grpc.*;
import net.spals.appbuilder.graph.model.ServiceGraph;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link GrpcWebServerModule}
 *
 * @author tkral
 */
public class GrpcWebServerModuleTest {

    @Test
    public void testCompressorRegistry() {
        final CompressorRegistry compressorRegistry = CompressorRegistry.newEmptyInstance();
        final ServerBuilder<?> grpcExternalServerBuilder = mock(ServerBuilder.class);

        final Module compressorRegistryModule = binder ->
            binder.bind(CompressorRegistry.class).toInstance(compressorRegistry);
        Guice.createInjector(
            createWebServerModule("testCompressorRegistry", grpcExternalServerBuilder),
            compressorRegistryModule
        );

        verify(grpcExternalServerBuilder).compressorRegistry(same(compressorRegistry));
    }

    @Test
    public void testDecompressorRegistry() {
        final DecompressorRegistry decompressorRegistry = DecompressorRegistry.emptyInstance();
        final ServerBuilder<?> grpcExternalServerBuilder = mock(ServerBuilder.class);

        final Module decompressorRegistryModule = binder ->
            binder.bind(DecompressorRegistry.class).toInstance(decompressorRegistry);
        Guice.createInjector(
            createWebServerModule("testDecompressorRegistry", grpcExternalServerBuilder),
            decompressorRegistryModule
        );

        verify(grpcExternalServerBuilder).decompressorRegistry(same(decompressorRegistry));
    }

    @Test
    public void testHandlerRegistry() {
        final HandlerRegistry handlerRegistry = mock(HandlerRegistry.class);
        final ServerBuilder<?> grpcExternalServerBuilder = mock(ServerBuilder.class);

        final Module handlerRegistryModule = binder ->
            binder.bind(HandlerRegistry.class).toInstance(handlerRegistry);
        Guice.createInjector(
            createWebServerModule("testHandlerRegistry", grpcExternalServerBuilder),
            handlerRegistryModule
        );

        verify(grpcExternalServerBuilder).fallbackHandlerRegistry(same(handlerRegistry));
    }

    @Test
    public void testServerTransportFilter() {
        final ServerTransportFilter serverTransportFilter = mock(ServerTransportFilter.class);
        final ServerBuilder<?> grpcExternalServerBuilder = mock(ServerBuilder.class);

        final Module serverTransportFilterModule = binder ->
            binder.bind(ServerTransportFilter.class).toInstance(serverTransportFilter);
        Guice.createInjector(
            createWebServerModule("testServerTransportFilter", grpcExternalServerBuilder),
            serverTransportFilterModule
        );

        verify(grpcExternalServerBuilder).addTransportFilter(same(serverTransportFilter));
    }

    private GrpcWebServerModule createWebServerModule(
        final String name,
        final ServerBuilder<?> grpcExternalServerBuilder
    ) {
        return new GrpcWebServerModule.Builder()
            .setApplicationName(name)
            .setServiceGraph(new ServiceGraph())
            .setGrpcExternalServerBuilder(grpcExternalServerBuilder)
            .build();
    }
}
