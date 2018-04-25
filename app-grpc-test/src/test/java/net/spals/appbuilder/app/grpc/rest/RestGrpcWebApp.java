package net.spals.appbuilder.app.grpc.rest;

import net.spals.appbuilder.app.grpc.GrpcWebApp;
import net.spals.appbuilder.config.service.ServiceScan;

import java.io.IOException;

/**
 * A {@link GrpcWebApp} which includes RESTful APIs.
 *
 * @author tkral
 */
public class RestGrpcWebApp extends GrpcWebApp {

    private static final int GRPC_PORT = 8080;

    public static void main(final String[] args) throws IOException, InterruptedException {
        final RestGrpcWebApp sampleGrpcWebApp = new RestGrpcWebApp();
        sampleGrpcWebApp.start();
        sampleGrpcWebApp.awaitTermination();
    }

    public RestGrpcWebApp() {
        super(GRPC_PORT);
    }

    @Override
    protected void configure(final Builder grpcWebAppBuilder) {
        grpcWebAppBuilder
            .enableRestServer()
            .setServiceScan(new ServiceScan.Builder()
                // Service packages should include any java_package used in the
                // gRPC protobuf file.
                .addServicePackages("net.spals.appbuilder.app.grpc.rest")
                .build());
    }
}
