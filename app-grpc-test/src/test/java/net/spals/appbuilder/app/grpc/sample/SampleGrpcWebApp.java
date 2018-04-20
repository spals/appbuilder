package net.spals.appbuilder.app.grpc.sample;

import net.spals.appbuilder.app.grpc.GrpcWebApp;

import java.io.IOException;

/**
 * A full sample {@link GrpcWebApp} which uses all default services
 * and bindings.
 *
 * @author tkral
 */
public class SampleGrpcWebApp extends GrpcWebApp {
    private static final int GRPC_PORT = 8080;

    public static void main(final String[] args) throws IOException, InterruptedException {
        final SampleGrpcWebApp sampleGrpcWebApp = new SampleGrpcWebApp(GRPC_PORT);
        sampleGrpcWebApp.start();
        sampleGrpcWebApp.awaitTermination();
    }

    public SampleGrpcWebApp(final int port) {
        super(port);
    }

    @Override
    protected void configure(final GrpcWebApp.Builder grpcWebAppBuilder) {  }
}
