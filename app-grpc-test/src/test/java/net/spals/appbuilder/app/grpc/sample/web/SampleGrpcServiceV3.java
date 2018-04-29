package net.spals.appbuilder.app.grpc.sample.web;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.grpc.sample.SampleRequestV3;
import net.spals.appbuilder.app.grpc.sample.SampleResponseV3;
import net.spals.appbuilder.app.grpc.sample.SampleServiceV3Grpc;

/**
 * A sample gRPC service.
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleGrpcServiceV3 extends SampleServiceV3Grpc.SampleServiceV3ImplBase {

    @Inject
    SampleGrpcServiceV3() { }

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
