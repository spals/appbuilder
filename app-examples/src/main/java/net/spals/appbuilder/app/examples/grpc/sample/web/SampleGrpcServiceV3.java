package net.spals.appbuilder.app.examples.grpc.sample.web;

import io.grpc.stub.StreamObserver;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.examples.grpc.sample.SampleRequestV3;
import net.spals.appbuilder.app.examples.grpc.sample.SampleResponseV3;
import net.spals.appbuilder.app.examples.grpc.sample.SampleServiceV3Grpc;

/**
 * A sample gRPC service using proto3.
 *
 * @author tkral
 */
@AutoBindSingleton
class SampleGrpcServiceV3 extends SampleServiceV3Grpc.SampleServiceV3ImplBase {

    @Override
    public void getSampleV3(
        final SampleRequestV3 request,
        final StreamObserver<SampleResponseV3> responseObserver
    ) {
        responseObserver.onNext(
            SampleResponseV3.newBuilder()
                .setIntField(3 * + request.getIntField())
                .setStringField(request.getStringField() + request.getStringField() + request.getStringField())
                .build()
        );
        responseObserver.onCompleted();
    }
}
