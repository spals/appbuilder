package net.spals.appbuilder.app.grpc;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import java.io.IOException;

/**
 * A test support class for starting and stopping
 * a {@link GrpcWebApp} at the start and end of a test class.
 *
 * @author tkral
 */
public class GrpcTestSupport {

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
