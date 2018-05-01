package net.spals.appbuilder.app.examples.grpc.sample.web;

import io.grpc.*;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sample gRPC {@link ServerInterceptor}.
 *
 * @author tkral
 */
@AutoBindSingleton
class SampleGrpcServerInterceptor implements ServerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleGrpcServerInterceptor.class);

    private final Metadata.Key<String> tokenKey =
        Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        final ServerCall<ReqT, RespT> call,
        final Metadata headers,
        final ServerCallHandler<ReqT, RespT> next
    ) {
        if (!headers.containsKey(tokenKey)) {
            call.close(Status.PERMISSION_DENIED.withDescription("no authorization token"), new Metadata());
            return new ServerCall.Listener<ReqT>(){};
        }

        final String authToken = headers.get(tokenKey);
        LOGGER.debug("Got authentication token (" + authToken + ")");
        return next.startCall(call, headers);
    }
}
