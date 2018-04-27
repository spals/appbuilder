package net.spals.appbuilder.app.grpc.rest;

import net.spals.appbuilder.app.grpc.GrpcWebApp;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.mapstore.core.MapStore;

import java.io.IOException;

/**
 * A {@link GrpcWebApp} which includes RESTful APIs.
 *
 * @author tkral
 */
public class RestGrpcWebApp extends GrpcWebApp {

    private static final String SERVICE_CONFIG_FILE_NAME = "config/rest-grpc-service.conf";
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
            .setServiceConfigFromClasspath(SERVICE_CONFIG_FILE_NAME)
            .setServiceScan(new ServiceScan.Builder()
                // Service packages should include any java_package used in the
                // gRPC protobuf file.
                .addServicePackages("net.spals.appbuilder.app.grpc.rest")
                .addDefaultServices(MapStore.class)
                .build());
    }
}
