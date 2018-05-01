package net.spals.appbuilder.app.examples.grpc.minimal;

import net.spals.appbuilder.app.grpc.GrpcWebApp;

import java.io.IOException;

/**
 * A minimally viable {@link GrpcWebApp}
 *
 * @author tkral
 */
public class MinimalGrpcWebApp extends GrpcWebApp {
    private static final int GRPC_PORT = 8080;

    public static void main(final String[] args) throws IOException, InterruptedException {
        final MinimalGrpcWebApp minimalGrpcWebApp = new MinimalGrpcWebApp();
        minimalGrpcWebApp.start();
        minimalGrpcWebApp.awaitTermination();
    }

    public MinimalGrpcWebApp() {
        super(GRPC_PORT);
    }

    @Override
    protected void configure(final GrpcWebApp.Builder grpcWebAppBuilder) {  }
}
