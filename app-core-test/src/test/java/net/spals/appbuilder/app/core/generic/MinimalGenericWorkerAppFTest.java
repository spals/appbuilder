package net.spals.appbuilder.app.core.generic;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.typesafe.config.ConfigFactory;
import io.opentracing.Tracer;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static com.googlecode.catchexception.CatchException.*;

/**
 * Functional tests for a minimal {@link GenericWorkerApp}
 *
 * @author tkral
 */
public class MinimalGenericWorkerAppFTest {
    private final Logger LOGGER = LoggerFactory.getLogger(MinimalGenericWorkerAppFTest.class);

    private final GenericWorkerApp minimalApp = new GenericWorkerApp.Builder("minimal", LOGGER).build();

    @Test
    public void testGenericWorkerAppLogger() {
        assertThat(minimalApp.getLogger(), sameInstance(LOGGER));
    }

    @Test
    public void testGenericWorkerAppName() {
        assertThat(minimalApp.getName(), is("minimal"));
    }

    @Test
    public void testMinimalServiceConfig() {
        assertThat(minimalApp.getServiceConfig(), is(ConfigFactory.empty()));
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
