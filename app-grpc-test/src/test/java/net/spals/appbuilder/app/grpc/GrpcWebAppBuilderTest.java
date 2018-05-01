package net.spals.appbuilder.app.grpc;

import io.grpc.ServerBuilder;
import net.spals.appbuilder.app.core.generic.GenericWorkerApp;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GrpcWebApp.Builder}
 *
 * @author tkral
 */
public class GrpcWebAppBuilderTest {

    @Test
    public void testDirectExecutor() {
        final ServerBuilder<?> grpcExternalServerBuilder = mock(ServerBuilder.class);
        final GrpcWebApp.Builder grpcWebAppBuilder = new GrpcWebApp.Builder(
            new GenericWorkerApp.Builder("testDirectExecutor", LoggerFactory.getLogger("testDirectExecutor")),
            grpcExternalServerBuilder,
            mock(GrpcWebApp.class)
        );

        grpcWebAppBuilder.directExecutor();
        verify(grpcExternalServerBuilder).directExecutor();
    }

    @Test
    public void testDisableErrorOnServiceLeaks() {
        final GenericWorkerApp.Builder appDelegateBuilder = mock(GenericWorkerApp.Builder.class);
        when(appDelegateBuilder.getName()).thenReturn("testDisableErrorOnServiceLeaks");

        final GrpcWebApp.Builder grpcWebAppBuilder = new GrpcWebApp.Builder(
            appDelegateBuilder,
            mock(ServerBuilder.class),
            mock(GrpcWebApp.class)
        );

        grpcWebAppBuilder.disableErrorOnServiceLeaks();
        verify(appDelegateBuilder).disableErrorOnServiceLeaks();
    }

    @Test
    public void testEnableBindingOverrides() {
        final GenericWorkerApp.Builder appDelegateBuilder = mock(GenericWorkerApp.Builder.class);
        when(appDelegateBuilder.getName()).thenReturn("testEnableBindingOverrides");

        final GrpcWebApp.Builder grpcWebAppBuilder = new GrpcWebApp.Builder(
            appDelegateBuilder,
            mock(ServerBuilder.class),
            mock(GrpcWebApp.class)
        );

        grpcWebAppBuilder.enableBindingOverrides();
        verify(appDelegateBuilder).enableBindingOverrides();
    }

    @Test
    public void testHandshakeTimeout() {
        final ServerBuilder<?> grpcExternalServerBuilder = mock(ServerBuilder.class);
        final GrpcWebApp.Builder grpcWebAppBuilder = new GrpcWebApp.Builder(
            new GenericWorkerApp.Builder("testHandshakeTimeout", LoggerFactory.getLogger("testHandshakeTimeout")),
            grpcExternalServerBuilder,
            mock(GrpcWebApp.class)
        );

        grpcWebAppBuilder.handshakeTimeout(1, TimeUnit.MINUTES);
        verify(grpcExternalServerBuilder).handshakeTimeout(1, TimeUnit.MINUTES);
    }

    @Test
    public void testUseTransportSecurity() {
        final ServerBuilder<?> grpcExternalServerBuilder = mock(ServerBuilder.class);
        final GrpcWebApp.Builder grpcWebAppBuilder = new GrpcWebApp.Builder(
            new GenericWorkerApp.Builder("testUseTransportSecurity", LoggerFactory.getLogger("testUseTransportSecurity")),
            grpcExternalServerBuilder,
            mock(GrpcWebApp.class)
        );

        final File certChain = new File("");
        final File privateKey = new File("");
        grpcWebAppBuilder.useTransportSecurity(certChain, privateKey);
        verify(grpcExternalServerBuilder).useTransportSecurity(same(certChain), same(privateKey));
    }
}
