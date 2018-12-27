package net.spals.appbuilder.app.grpc;

import net.spals.appbuilder.app.examples.grpc.sample.SampleGrpcWebApp;
import net.spals.appbuilder.filestore.core.FileStore;
import net.spals.appbuilder.mapstore.core.MapStore;
import org.mockito.internal.util.MockUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Functional tests for a sample {@link GrpcWebAppWithMocks}
 *
 * @author tkral
 */
public class GrpcWebAppWithMocksFTest {

    private final GrpcWebApp sampleAppWithMocks = GrpcWebAppWithMocks.grpcWithMocks(new SampleGrpcWebApp())
        .addMockSingleton(mock(MapStore.class), MapStore.class);
    private final GrpcTestSupport testServerWrapper = GrpcTestSupport.embeddedGrpc(sampleAppWithMocks);

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
        assertThat(sampleAppWithMocks.isRunning(), is(false));
    }

    @Test
    public void testInjectedMocks() {
        // Assert that the mocked MapStore is present in the service graph.
        assertThat(MockUtil.isMock(
            sampleAppWithMocks.getServiceInjector().getInstance(MapStore.class)
        ), is(true));
    }

    @Test
    public void testNonInjectedMocks() {
        // Assert that anything not mocked is left alone
        assertThat(MockUtil.isMock(
            sampleAppWithMocks.getServiceInjector().getInstance(FileStore.class)
        ), is(false));
    }
}
