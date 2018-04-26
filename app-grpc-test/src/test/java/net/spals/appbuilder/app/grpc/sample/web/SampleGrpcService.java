package net.spals.appbuilder.app.grpc.sample.web;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.grpc.sample.SampleRequest;
import net.spals.appbuilder.app.grpc.sample.SampleResponse;
import net.spals.appbuilder.app.grpc.sample.SampleServiceGrpc;

/**
 * A sample gRPC service.
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleGrpcService extends SampleServiceGrpc.SampleServiceImplBase {

    @Inject
    SampleGrpcService() { }

    @Override
    public void getSample(
        final SampleRequest request,
        final StreamObserver<SampleResponse> responseObserver
    ) {
        responseObserver.onNext(
            SampleResponse.newBuilder()
                .setIntField(request.getIntField() + request.getIntField())
                .setStringField(request.getStringField() + request.getStringField())
                .build()
        );
        responseObserver.onCompleted();
    }
}