package net.spals.appbuilder.app.grpc;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.opentracing.Tracer;
import net.spals.appbuilder.app.grpc.minimal.MinimalGrpcWebApp;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Functional tests for a minimal {@link GrpcWebApp}
 *
 * @author tkral
 */
public class MinimalGrpcWebAppFTest {

    private final GrpcWebApp minimalApp = new MinimalGrpcWebApp();
    private final GrpcTestSupport testServerWrapper = GrpcTestSupport.embeddedGrpc(minimalApp);

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
    }

    @Test
    public void testGrpcWebAppLogger() {
        assertThat(minimalApp.getLogger(), notNullValue());
    }

    @Test
    public void testGrpcWebAppName() {
        assertThat(minimalApp.getName(), is("MinimalGrpcWebApp"));
    }

    @DataProvider
    Object[][] noDefaultServiceInjectionProvider() {
        return new Object[][] {
            {TypeLiteral.get(ExecutorServiceFactory.class)},
            {TypeLiteral.get(Tracer.class)},
        };
    }

    @Test(dataProvider = "noDefaultServiceInjectionProvider")
    public void testNoDefaultServiceInjection(final TypeLiteral<?> typeLiteral) {
        final Injector serviceInjector = minimalApp.getServiceInjector();
        verifyException(() -> serviceInjector.getInstance(Key.get(typeLiteral)), ConfigurationException.class);
    }
}
