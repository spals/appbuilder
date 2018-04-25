package net.spals.appbuilder.app.grpc;

import io.grpc.Channel;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * A test support class for starting and stopping
 * a {@link GrpcWebApp} at the start and end of a test class.
 *
 * @author tkral
 */
public class GrpcTestSupport {

    private final Channel channel;
    private final GrpcWebApp grpcWebApp;

    public static GrpcTestSupport embeddedGrpc(final GrpcWebApp grpcWebApp) {
        final String serverName = grpcWebApp.getClass().getSimpleName();
        return new GrpcTestSupport(
            grpcWebApp,
            InProcessServerBuilder.forName(serverName),
            InProcessChannelBuilder.forName(serverName).build()
        );
    }

    public static GrpcTestSupport nettyGrpc(final GrpcWebApp grpcWebApp) {
        try (final ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            final int port = socket.getLocalPort();

            return new GrpcTestSupport(
                grpcWebApp,
                NettyServerBuilder.forPort(port),
                NettyChannelBuilder.forAddress("127.0.0.1", port)
                    .usePlaintext()
                    .build()
            );
        } catch (IOException e) {
            throw new RuntimeException("Unable to find free port", e);
        }
    }

    private GrpcTestSupport(
        final GrpcWebApp grpcWebApp,
        final ServerBuilder<?> grpcExternalServerBuilder,
        final Channel channel
    ) {
        this.grpcWebApp = grpcWebApp;
        this.grpcWebApp.grpcWebAppBuilder.setGrpcExternalServerBuilder(grpcExternalServerBuilder);
        this.channel = channel;
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

    public Channel getChannel() {
        return channel;
    }
}
