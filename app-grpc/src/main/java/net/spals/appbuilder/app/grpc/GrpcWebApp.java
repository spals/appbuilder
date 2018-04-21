package net.spals.appbuilder.app.grpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.typesafe.config.Config;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.WebAppBuilder;
import net.spals.appbuilder.app.core.generic.GenericWorkerApp;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
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
    private final AtomicReference<Server> serverDelegateRef = new AtomicReference<>();

    private final AtomicBoolean isConfigured = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @VisibleForTesting
    final GrpcWebApp.Builder grpcWebAppBuilder;

    protected GrpcWebApp(final int port) {
        this(Optional.empty(), ServerBuilder.forPort(port));
    }

    protected GrpcWebApp(final Logger logger, final int port) {
        this(Optional.of(logger), ServerBuilder.forPort(port));
    }

    private GrpcWebApp(final Optional<Logger> loggerOpt, final ServerBuilder<?> serverBuilder) {
        grpcWebAppBuilder = new GrpcWebApp.Builder(
            getClass().getSimpleName() /* name */,
            loggerOpt.orElseGet(() -> LoggerFactory.getLogger(getClass().getSimpleName())),
            serverBuilder,
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
    Server getServerDelegate() {
        return serverDelegateRef.get();
    }

    // ========== Grpc Server ==========

    public final void awaitTermination() throws InterruptedException {
        runConfigure().serverDelegateRef.get().awaitTermination();
    }

    public final int getPort() {
        return runConfigure().serverDelegateRef.get().getPort();
    }

    @VisibleForTesting
    final void shutdown() {
        runConfigure().serverDelegateRef.get().shutdown();
    }

    final void shutdownNow() {
        runConfigure().serverDelegateRef.get().shutdownNow();
    }

    public synchronized final void start() throws IOException {
        if (!isRunning.getAndSet(true)) {
            runConfigure().serverDelegateRef.get().start();
            getLogger().info("gRPC Server started (listening on " + getPort() + ")");
            Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                    System.err.println("*** shutting down gRPC server due to JVM shutdown");
                    serverDelegateRef.get().shutdown();
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
        private ServerBuilder<?> serverDelegateBuilder;

        private final GrpcWebApp grpcWebApp;

        private final GrpcWebServerModule.Builder webServerModuleBuilder =
            new GrpcWebServerModule.Builder();

        private Builder(
            final String name,
            final Logger logger,
            final ServerBuilder<?> serverBuilder,
            final GrpcWebApp grpcWebApp
        ) {
            appDelegateBuilder = new GenericWorkerApp.Builder(name, logger);
            setServerBuilder(serverBuilder);

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
            serverDelegateBuilder.directExecutor();
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

        @Override
        public Builder enableServiceGraph(final ServiceGraphFormat graphFormat) {
            appDelegateBuilder.enableServiceGraph(graphFormat);
            return this;
        }

        /**
         * {@link ServerBuilder#handshakeTimeout(long, TimeUnit)}
         */
        public Builder handshakeTimeout(final long timeout, final TimeUnit unit) {
            serverDelegateBuilder.handshakeTimeout(timeout, unit);
            return this;
        }

        @VisibleForTesting
        Builder setServerBuilder(final ServerBuilder<?> serverBuilder) {
            serverDelegateBuilder = serverBuilder;
            webServerModuleBuilder.setServerBuilder(serverBuilder);
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
            return this;
        }

        /**
         * {@link ServerBuilder#useTransportSecurity(File, File)}
         */
        public Builder useTransportSecurity(final File certChain, final File privateKey) {
            serverDelegateBuilder.useTransportSecurity(certChain, privateKey);
            return this;
        }

        @Override
        public GrpcWebApp build() {
            webServerModuleBuilder.setServiceGraph(appDelegateBuilder.getServiceGraph());
            appDelegateBuilder.addModule(webServerModuleBuilder.build());

            final GenericWorkerApp appDelegate = appDelegateBuilder.build();
            grpcWebApp.appDelegateRef.set(appDelegate);

            // Automatically register a managed cached thread pool, if possible
            registerServerExecutor(appDelegate.getServiceInjector());
            grpcWebApp.serverDelegateRef.set(serverDelegateBuilder.build());

            return grpcWebApp;
        }

        private void registerServerExecutor(final Injector serviceInjector) {
            final Key<ExecutorServiceFactory> executorServiceBindingKey = Key.get(ExecutorServiceFactory.class);
            Optional.ofNullable(serviceInjector.getExistingBinding(executorServiceBindingKey))
                .map(binding -> serviceInjector.getInstance(binding.getKey()))
                .map(executorServiceFactory -> {
                    final ExecutorServiceFactory.Key grpcExecutorKey =
                        new ExecutorServiceFactory.Key.Builder(serverDelegateBuilder.getClass()).build();
                    return executorServiceFactory.createCachedThreadPool(grpcExecutorKey);
                })
                .ifPresent(executor -> serverDelegateBuilder.executor(executor));
        }
    }
}
