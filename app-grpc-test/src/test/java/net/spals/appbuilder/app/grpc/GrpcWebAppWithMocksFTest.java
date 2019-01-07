package net.spals.appbuilder.app.grpc;

import net.spals.appbuilder.app.examples.grpc.minimal.MinimalGrpcWebApp;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import org.mockito.internal.util.MockUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Functional tests for a sample {@link GrpcWebAppWithMocks}
 *
 * @author tkral
 */
public class GrpcWebAppWithMocksFTest {

    private final GrpcWebApp minimalAppWithMocks = GrpcWebAppWithMocks.grpcWithMocks(new MinimalGrpcWebApp())
        .addMockSingleton(mock(ExecutorServiceFactory.class), ExecutorServiceFactory.class);
    private final GrpcTestSupport testServerWrapper = GrpcTestSupport.nettyGrpc(minimalAppWithMocks);

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
        assertThat(minimalAppWithMocks.isRunning(), is(false));
    }

    @Test
    public void testPositiveGrpcPortNumber() {
        // This is really more of a test for GrpcTestSupport, but let's make
        // sure that the 0 port number is actually overwritten.
        assertThat(minimalAppWithMocks.getGrpcPort(), greaterThan(0));
    }

    @Test
    public void testInjectedMocks() {
        // Assert that the mocked ExecutorServiceFactory is present in the service graph.
        assertThat(MockUtil.isMock(
            minimalAppWithMocks.getServiceInjector().getInstance(ExecutorServiceFactory.class)
        ), is(true));
    }
}
