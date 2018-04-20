package net.spals.appbuilder.app.grpc;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link GrpcWebApp} which creates an in-process
 * gRPC {@link io.grpc.Server} and delegates its
 * configuration step for testing purposes.
 *
 * @author tkral
 */
public class GrpcTestSupport /*extends GrpcWebApp*/ {

    private final GrpcWebApp grpcWebApp;

    public GrpcTestSupport(final GrpcWebApp grpcWebApp) {
        this.grpcWebApp = grpcWebApp;

        // Inject a test server builder into the gRPC web app
        final ServerBuilder<?> testServerBuilder =
            InProcessServerBuilder.forName(grpcWebApp.getClass().getSimpleName());
        this.grpcWebApp.grpcWebAppBuilder.setServerBuilder(testServerBuilder);
    }

    public void after() {
        grpcWebApp.shutdownNow();
    }

    public void before() {
        try {
            grpcWebApp.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
