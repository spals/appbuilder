package net.spals.appbuilder.app.examples.grpc.sample.web;

import io.grpc.stub.StreamObserver;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.examples.grpc.sample.SampleRequestV2;
import net.spals.appbuilder.app.examples.grpc.sample.SampleResponseV2;
import net.spals.appbuilder.app.examples.grpc.sample.SampleServiceV2Grpc;

/**
 * A sample gRPC service using proto2.
 *
 * @author tkral
 */
@AutoBindSingleton
class SampleGrpcServiceV2 extends SampleServiceV2Grpc.SampleServiceV2ImplBase {

    @Override
    public void getSampleV2(
        final SampleRequestV2 request,
        final StreamObserver<SampleResponseV2> responseObserver
    ) {
        responseObserver.onNext(
            SampleResponseV2.newBuilder()
                .setIntField(2 * + request.getIntField())
                .setStringField(request.getStringField() + request.getStringField())
                .build()
        );
        responseObserver.onCompleted();
    }
}
