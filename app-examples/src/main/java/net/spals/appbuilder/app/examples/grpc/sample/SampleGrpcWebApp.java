package net.spals.appbuilder.app.examples.grpc.sample;

import net.spals.appbuilder.app.grpc.GrpcWebApp;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.filestore.core.FileStore;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.keystore.core.KeyStore;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.message.core.MessageConsumer;
import net.spals.appbuilder.message.core.MessageProducer;
import net.spals.appbuilder.model.core.ModelSerializer;

import java.io.IOException;

/**
 * A full sample {@link GrpcWebApp} which uses all default services
 * and bindings.
 *
 * @author tkral
 */
public class SampleGrpcWebApp extends GrpcWebApp {

    private static final String SERVICE_CONFIG_FILE_NAME = "config/sample-grpc-service.conf";
    private static final int GRPC_PORT = 8080;

    public static void main(final String[] args) throws IOException, InterruptedException {
        final SampleGrpcWebApp sampleGrpcWebApp = new SampleGrpcWebApp();
        sampleGrpcWebApp.start();
        sampleGrpcWebApp.awaitTermination();
    }

    public SampleGrpcWebApp() {
        super(GRPC_PORT);
    }

    @Override
    protected void configure(final Builder grpcWebAppBuilder) {
        grpcWebAppBuilder
            .enableServiceGraph(ServiceGraphFormat.TEXT)
            .setServiceConfigFromClasspath(SERVICE_CONFIG_FILE_NAME)
            .setServiceScan(new ServiceScan.Builder()
                .addServicePackages("net.spals.appbuilder.app.examples.grpc.sample")
                .addDefaultServices(FileStore.class)
                .addDefaultServices(KeyStore.class)
                .addDefaultServices(MapStore.class)
                .addDefaultServices(MessageConsumer.class, MessageProducer.class)
                .addDefaultServices(ModelSerializer.class)
                .build())
            .addModule(new SampleGrpcGuiceModule());
    }
}
