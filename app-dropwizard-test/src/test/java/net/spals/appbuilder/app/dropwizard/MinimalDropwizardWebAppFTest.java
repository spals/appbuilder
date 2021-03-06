package net.spals.appbuilder.app.dropwizard;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.opentracing.Tracer;
import net.spals.appbuilder.app.examples.dropwizard.minimal.MinimalDropwizardWebApp;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.validation.Validator;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Functional tests for a minimal {@link DropwizardWebApp}
 *
 * @author tkral
 */
public class MinimalDropwizardWebAppFTest {

    private final DropwizardTestSupport<Configuration> testServerWrapper =
        new DropwizardTestSupport<>(
            MinimalDropwizardWebApp.class,
            "",
            ConfigOverride.config("server.applicationConnectors[0].port", "0")
        );
    private DropwizardWebApp webAppDelegate;

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
        webAppDelegate = ((MinimalDropwizardWebApp)testServerWrapper.getApplication()).getDelegate();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
    }

    @Test
    public void testDropwizardWebAppLogger() {
        assertThat(webAppDelegate.getLogger(), notNullValue());
    }

    @Test
    public void testDropwizardWebAppName() {
        assertThat(webAppDelegate.getName(), is("MinimalDropwizardWebApp"));
    }

    @DataProvider
    Object[][] defaultServiceInjectionProvider() {
        return new Object[][] {
            {TypeLiteral.get(Environment.class)},
            {TypeLiteral.get(HealthCheckRegistry.class)},
            {TypeLiteral.get(MetricRegistry.class)},
            {TypeLiteral.get(Validator.class)},
        } ;
    }

    @Test(dataProvider = "defaultServiceInjectionProvider")
    public void testDefaultServiceInjection(final TypeLiteral<?> typeLiteral) {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(Key.get(typeLiteral)), notNullValue());
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
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        verifyException(() -> serviceInjector.getInstance(Key.get(typeLiteral)), ConfigurationException.class);
    }
}
