package net.spals.appbuilder.app.grpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.typesafe.config.Config;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.WebAppBuilder;
import net.spals.appbuilder.app.core.generic.GenericWorkerApp;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A wrapper for {@link App} which
 * uses gRPC as the application
 * framework.
 *
 * @author tkral
 */
public abstract class GrpcWebApp implements App {

    private final AtomicReference<App> appDelegateRef = new AtomicReference<>();
    private final AtomicReference<Server> grpcExternalServerRef = new AtomicReference<>();

    private final AtomicReference<Optional<Server>> grpcInternalServerRef =
        new AtomicReference<>(Optional.empty());
    private final AtomicReference<Optional<ResourceConfig>> restResourceConfigRef =
        new AtomicReference<>(Optional.empty());
    private final AtomicReference<Optional<io.netty.channel.Channel>> restServerRef =
        new AtomicReference<>(Optional.empty());

    private final AtomicBoolean isConfigured = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    @VisibleForTesting
    final GrpcWebApp.Builder grpcWebAppBuilder;

    protected GrpcWebApp(final int grpcPort) {
        grpcWebAppBuilder = new GrpcWebApp.Builder(
            getClass().getSimpleName() /* name */,
            LoggerFactory.getLogger(getClass()),
            ServerBuilder.forPort(grpcPort),
            this
        );
    }

    protected abstract void configure(final GrpcWebApp.Builder grpcWebAppBuilder);

    synchronized GrpcWebApp runConfigure() {
        if (!isConfigured.getAndSet(true)) {
            configure(grpcWebAppBuilder);
            return grpcWebAppBuilder.build();
        }

        return this;
    }

    @VisibleForTesting
    Server getGrpcExternalServer() {
        return grpcExternalServerRef.get();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    // ========== Grpc Server ==========

    public final void awaitTermination() throws InterruptedException {
        runConfigure().grpcExternalServerRef.get().awaitTermination();
    }

    public final int getGrpcPort() {
        return runConfigure().grpcExternalServerRef.get().getPort();
    }

    public final int getRestPort() {
        // By convention, the rest post is 10 off from the grpc port.
        // We add 10 here to give tests enough room to allocate free ports.
        // Otherwise, we were constantly running into port conflicts
        return getGrpcPort() + 10;
    }

    @VisibleForTesting
    synchronized final void shutdown() {
        if (isRunning.getAndSet(false)) {
            isStarted.set(false);
            restServerRef.get().ifPresent(restServer -> restServer.close());

            grpcInternalServerRef.get().ifPresent(grpcInternalServer -> grpcInternalServer.shutdown());
            runConfigure().grpcExternalServerRef.get().shutdown();
        }
    }

    @VisibleForTesting
    synchronized final void shutdownNow() {
        if (isRunning.getAndSet(false)) {
            isStarted.set(false);

            restServerRef.get().ifPresent(restServer -> restServer.close());
            grpcInternalServerRef.get().ifPresent(grpcInternalServer -> grpcInternalServer.shutdownNow());
            runConfigure().grpcExternalServerRef.get().shutdownNow();
        }
    }

    public synchronized final void start() throws IOException {
        if (!isStarted.getAndSet(true)) {
            final Server grpcExternalServer = runConfigure().grpcExternalServerRef.get();
            grpcExternalServer.start();
            getLogger().info("gRPC external server started (listening on " + getGrpcPort() + ")");

            if (grpcInternalServerRef.get().isPresent()) {
                grpcInternalServerRef.get().get().start();
                getLogger().info("gRPC internal server started");
            }

            restResourceConfigRef.get().ifPresent(resourceConfig -> {
                final int restPort = getRestPort();
                final URI baseUri = UriBuilder.fromUri("http://localhost/").port(restPort).build();
                final io.netty.channel.Channel restServer =
                    NettyHttpContainerProvider.createHttp2Server(baseUri, resourceConfig,null);
                getLogger().info("RESTful external server started (listening on " + restPort + ")");
                restServerRef.set(Optional.of(restServer));
            });

            isRunning.set(true);
            Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                    System.err.println("*** shutting down gRPC web app due to JVM shutdown");
                    shutdown();
                    System.err.println("*** gRPC server shut down");

                })
            );
        }
    }

    // ========== Spals App ==========

    @Override
    public final Logger getLogger() {
        return runConfigure().appDelegateRef.get().getLogger();
    }

    @Override
    public final String getName() {
        return runConfigure().appDelegateRef.get().getName();
    }

    @Override
    public final Config getServiceConfig() {
        return runConfigure().appDelegateRef.get().getServiceConfig();
    }

    @Override
    public final Injector getServiceInjector() {
        return runConfigure().appDelegateRef.get().getServiceInjector();
    }

    public static class Builder implements WebAppBuilder<GrpcWebApp> {

        private final GenericWorkerApp.Builder appDelegateBuilder;

        private final GrpcWebApp grpcWebApp;

        private final GrpcWebServerModule.Builder webServerModuleBuilder =
            new GrpcWebServerModule.Builder();

        private Builder(
            final String name,
            final Logger logger,
            final ServerBuilder<?> grpcExternalServerBuilder,
            final GrpcWebApp grpcWebApp
        ) {
            appDelegateBuilder = new GenericWorkerApp.Builder(name, logger);
            webServerModuleBuilder.setApplicationName(name);
            setGrpcExternalServerBuilder(grpcExternalServerBuilder);

            this.grpcWebApp = grpcWebApp;
        }

        @Override
        public Builder addModule(final Module module) {
            appDelegateBuilder.addModule(module);
            return this;
        }

        /**
         * {@link ServerBuilder#directExecutor()}
         */
        public Builder directExecutor() {
            webServerModuleBuilder.getGrpcExternalServerBuilder().directExecutor();
            return this;
        }

        @Override
        public Builder disableErrorOnServiceLeaks() {
            appDelegateBuilder.disableErrorOnServiceLeaks();
            return this;
        }

        @Override
        public Builder disableWebServerAutoBinding() {
            webServerModuleBuilder.setWebServerAutoBindingEnabled(false);
            return this;
        }

        @Override
        public Builder enableBindingOverrides() {
            appDelegateBuilder.enableBindingOverrides();
            return this;
        }

        @Override
        public Builder enableRequestScoping() {
            throw new UnsupportedOperationException("Coming soon.");
        }

        public Builder enableRestServer() {
            webServerModuleBuilder.enableRestServer(
                InProcessServerBuilder.forName(appDelegateBuilder.getName()),
                new ResourceConfig()
            );
            return this;
        }

        @Override
        public Builder enableServiceGraph(final ServiceGraphFormat graphFormat) {
            appDelegateBuilder.enableServiceGraph(graphFormat);
            return this;
        }

        /**
         * {@link ServerBuilder#handshakeTimeout(long, TimeUnit)}
         */
        public Builder handshakeTimeout(final long timeout, final TimeUnit unit) {
            webServerModuleBuilder.getGrpcExternalServerBuilder().handshakeTimeout(timeout, unit);
            return this;
        }

        @VisibleForTesting
        Builder setGrpcExternalServerBuilder(final ServerBuilder<?> grpcExternalServerBuilder) {
            webServerModuleBuilder.setGrpcExternalServerBuilder(grpcExternalServerBuilder);
            return this;
        }

        @Override
        public Builder setServiceConfig(final Config serviceConfig) {
            appDelegateBuilder.setServiceConfig(serviceConfig);
            return this;
        }

        @Override
        public Builder setServiceConfigFromClasspath(final String serviceConfigFileName) {
            appDelegateBuilder.setServiceConfigFromClasspath(serviceConfigFileName);
            return this;
        }

        @Override
        public Builder setServiceScan(final ServiceScan serviceScan) {
            appDelegateBuilder.setServiceScan(serviceScan);
            webServerModuleBuilder.setServiceScan(serviceScan);
            return this;
        }

        /**
         * {@link ServerBuilder#useTransportSecurity(File, File)}
         */
        public Builder useTransportSecurity(final File certChain, final File privateKey) {
            webServerModuleBuilder.getGrpcExternalServerBuilder().useTransportSecurity(certChain, privateKey);
            return this;
        }

        @Override
        public GrpcWebApp build() {
            webServerModuleBuilder.setServiceGraph(appDelegateBuilder.getServiceGraph());
            final GrpcWebServerModule webServerModule = webServerModuleBuilder.build();
            appDelegateBuilder.addModule(webServerModule);

            final GenericWorkerApp appDelegate = appDelegateBuilder.build();
            grpcWebApp.appDelegateRef.set(appDelegate);

            // Automatically register a managed cached thread pool, if possible
            registerGrpcServerExecutor(appDelegate.getServiceInjector());
            // Build the external gRPC server
            final Server grpcExternalServer = webServerModule.getGrpcExternalServerBuilder().build();
            grpcWebApp.grpcExternalServerRef.set(grpcExternalServer);
            // If necessary, build the internal gRPC server
            grpcWebApp.grpcInternalServerRef.set(
                webServerModule.getGrpcInternalServerBuilder().map(grpcInternalServerBuilder -> grpcInternalServerBuilder.build())
            );
            grpcWebApp.restResourceConfigRef.set(webServerModule.getRestResourceConfig());

            return grpcWebApp;
        }

        private void registerGrpcServerExecutor(final Injector serviceInjector) {
            final Key<ExecutorServiceFactory> executorServiceBindingKey = Key.get(ExecutorServiceFactory.class);
            Optional.ofNullable(serviceInjector.getExistingBinding(executorServiceBindingKey))
                .map(binding -> serviceInjector.getInstance(binding.getKey()))
                .map(executorServiceFactory -> {
                    final ExecutorServiceFactory.Key grpcExecutorKey =
                        new ExecutorServiceFactory.Key.Builder(webServerModuleBuilder.getGrpcExternalServerBuilder().getClass()).build();
                    return executorServiceFactory.createCachedThreadPool(grpcExecutorKey);
                })
                .ifPresent(executor -> {
                    webServerModuleBuilder.getGrpcExternalServerBuilder().executor(executor);
                    webServerModuleBuilder.getGrpcInternalServerBuilder().ifPresent(grpcInternalServerBuilder ->
                        grpcInternalServerBuilder.executor(executor));
                });
        }
    }
}
