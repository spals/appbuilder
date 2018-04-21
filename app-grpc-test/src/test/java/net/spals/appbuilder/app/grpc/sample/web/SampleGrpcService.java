package net.spals.appbuilder.app.grpc.sample.web;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.grpc.sample.SampleRequest;
import net.spals.appbuilder.grpc.sample.SampleResponse;
import net.spals.appbuilder.grpc.sample.SampleRouteServiceGrpc;

/**
 * A sample gRPC service.
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleGrpcService extends SampleRouteServiceGrpc.SampleRouteServiceImplBase {

    @Inject
    SampleGrpcService() { }

    @Override
    public void getSample(
        final SampleRequest request,
        final StreamObserver<SampleResponse> responseObserver
    ) {
        responseObserver.onNext(
            SampleResponse.newBuilder()
                .setIntField(request.getIntField())
                .setStringField(request.getStringField())
                .build()
        );
        responseObserver.onCompleted();
    }
}
