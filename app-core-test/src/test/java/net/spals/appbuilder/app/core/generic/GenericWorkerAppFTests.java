package net.spals.appbuilder.app.core.generic;

import com.google.inject.CreationException;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * A series of functional tests for {@link GenericWorkerApp}
 * which do not fit into the minimal or sample test classes.
 *
 * @author tkral
 */
public class GenericWorkerAppFTests {
    private final Logger LOGGER = LoggerFactory.getLogger(GenericWorkerAppFTests.class);

    @Test
    public void testModuleOverrideError() {
        // This doesn't really test anything in the AppBuilder code
        // but just proves out how Guice errors out on override bindings
        catchException(() -> new GenericWorkerApp.Builder("testModuleOverrideError", LOGGER)
            .addModule(binder -> binder.bind(MySingletonService.class)
                .toInstance(new MySingletonServiceA()))
            .addModule(binder -> binder.bind(MySingletonService.class)
                .toInstance(new MySingletonServiceB()))
            .build());

        assertThat(caughtException(), instanceOf(CreationException.class));
    }

    @DataProvider
    Object[][] enableBindingOverridesProvider() {
        final MySingletonService serviceA = new MySingletonServiceA();
        final MySingletonService serviceB = new MySingletonServiceB();

        return new Object[][] {
                // Ensure that binding order is honored. The last one in is chosen
                {serviceA, serviceB},
                {serviceB, serviceA},
        };
    }

    @Test(dataProvider = "enableBindingOverridesProvider")
    public void testEnableBindingOverrides(final MySingletonService firstService,
                                           final MySingletonService secondService) {
        final GenericWorkerApp app = new GenericWorkerApp.Builder(
                "testEnableBindingOverrides", LOGGER)
            .addModule(binder -> binder.bind(MySingletonService.class)
                .toInstance(firstService))
            .addModule(binder -> binder.bind(MySingletonService.class)
                .toInstance(secondService))
            .enableBindingOverrides()
            .build();

        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(MySingletonService.class), is(secondService));
    }

    private interface MySingletonService {  }

    private class MySingletonServiceA implements MySingletonService {  }

    private class MySingletonServiceB implements MySingletonService {  }
}