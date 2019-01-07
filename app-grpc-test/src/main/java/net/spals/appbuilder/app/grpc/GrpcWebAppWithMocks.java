package net.spals.appbuilder.app.grpc;

import com.google.inject.Module;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A framework for injecting mock services into a
 * {@link GrpcWebApp}.
 *
 * Example:
 *
 * final MyGrpcAppWithMocks myGrpcAppWithMocks = GrpcWebAppWithMocks.grpcWithMocks(new MyGrpcApp(0))
 *     .addMockSingleton(mockedDatabase, Database.class)
 *     .addMockSingleton(mockedNotifier, Notifier.class)
 *
 * GrpcTestSupport.embeddedGrpc(myGrpcAppWithMocks).before()
 *
 * @author tkral
 */
public class GrpcWebAppWithMocks extends GrpcWebApp {

    private final GrpcWebApp delegateWebApp;
    private final List<Module> mockSingletonModules = new ArrayList<>();

    public static GrpcWebAppWithMocks grpcWithMocks(final GrpcWebApp grpcWebApp) {
        return new GrpcWebAppWithMocks(grpcWebApp);
    }

    private GrpcWebAppWithMocks(
        final GrpcWebApp delegateWebApp
    ) {
        // Note: We don't forward the gRPC port from the delegate.
        // This is because reading the port requires the gRPC server
        // to actually be running (which it likely is not). Also, this
        // class will be used in conjunction with GrpcTestSupport which
        // will overwrite the port anyway.
        super(0);
        this.delegateWebApp = delegateWebApp;
    }

    public <I, M extends I> GrpcWebAppWithMocks addMockSingleton(final M mockedService, final Class<I> baseClass) {
        final Module mockSingletonModule = binder -> binder.bind(baseClass).toInstance(mockedService);
        mockSingletonModules.add(mockSingletonModule);
        return this;
    }

    @Override
    protected void configure(final GrpcWebApp.Builder grpcWebAppBuilder) {
        delegateWebApp.configure(grpcWebAppBuilder);

        grpcWebAppBuilder.enableBindingOverrides();
        mockSingletonModules.forEach(mockSingletonModule -> grpcWebAppBuilder.addModule(mockSingletonModule));
    }
}
