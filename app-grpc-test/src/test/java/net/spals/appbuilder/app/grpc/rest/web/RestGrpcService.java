package net.spals.appbuilder.app.grpc.rest.web;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.grpc.rest.RestGetRequest;
import net.spals.appbuilder.app.grpc.rest.RestGetResponse;
import net.spals.appbuilder.app.grpc.rest.RestServiceGrpc;

/**
 * A sample gRPC service which has a complementary RESTful interface.
 *
 * @author tkral
 */
@AutoBindSingleton
public class RestGrpcService extends RestServiceGrpc.RestServiceImplBase {

    @Inject
    RestGrpcService() { }

    @Override
    public void getRest(
        final RestGetRequest request,
        final StreamObserver<RestGetResponse> responseObserver
    ) {
        responseObserver.onNext(
            RestGetResponse.newBuilder()
                .setId(3 * request.getId())
                .build()
        );
        responseObserver.onCompleted();
    }
}
