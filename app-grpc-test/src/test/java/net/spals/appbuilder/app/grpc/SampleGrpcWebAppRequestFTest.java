package net.spals.appbuilder.app.grpc;

import net.spals.appbuilder.app.grpc.sample.SampleGrpcWebApp;
import net.spals.appbuilder.grpc.sample.SampleRequest;
import net.spals.appbuilder.grpc.sample.SampleResponse;
import net.spals.appbuilder.grpc.sample.SampleRouteServiceGrpc;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Functional tests for requests against a sample {@link GrpcWebApp}
 *
 * @author tkral
 */
public class SampleGrpcWebAppRequestFTest {

    private final GrpcWebApp sampleApp = new SampleGrpcWebApp();
    private final GrpcTestSupport testServerWrapper = GrpcTestSupport.nettyGrpc(sampleApp);

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
    }

    @Test
    public void testGrpcServiceRequest() {
        final SampleRouteServiceGrpc.SampleRouteServiceBlockingStub stub =
            SampleRouteServiceGrpc.newBlockingStub(testServerWrapper.getChannel());

        final SampleRequest request = SampleRequest.newBuilder()
            .setIntField(1).setStringField("myString").build();
        final SampleResponse response = stub.getSample(request);

        assertThat(response.getIntField(), is(2));
        assertThat(response.getStringField(), is("myStringmyString"));
    }
}
