package net.spals.appbuilder.app.core.generic;

import com.google.common.collect.ImmutableSet;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.config.service.ServiceScan;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Functional tests for binding overrides within a {@link GenericWorkerApp}
 *
 * @author tkral
 */
public class GenericWorkerAppOverridesFTest {
    private final Logger LOGGER = LoggerFactory.getLogger(GenericWorkerAppOverridesFTest.class);

    @Test
    public void testBindingOverrideError() {
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
    Object[][] bindingOverridesProvider() {
        final MySingletonService serviceA = new MySingletonServiceA();
        final MySingletonService serviceB = new MySingletonServiceB();

        return new Object[][] {
                // Ensure that binding order is honored. The last one in is chosen
                {serviceA, serviceB},
                {serviceB, serviceA},
        };
    }

    @Test(dataProvider = "bindingOverridesProvider")
    public void testBindingOverrides(final MySingletonService firstService,
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

    @Test
    public void testCustomBindingOverridesAutoBind() {
        final ServiceScan serviceScan = createServiceScan();

        final GenericWorkerApp app = new GenericWorkerApp.Builder(
                "testEnableBindingOverrides", LOGGER)
            .setServiceScan(serviceScan)
            .addModule(binder -> binder.bind(MySingletonService.class)
                .toInstance(new MySingletonServiceA()))
            .enableBindingOverrides()
            .build();

        // Assert that the binding override uses the service from the custom module,
        // not the auto-bound service because the custom module came second.
        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(MySingletonService.class),
                instanceOf(MySingletonServiceA.class));
    }

    @Test
    public void testCustomBindingOverridesAutoBind2() {
        final ServiceScan serviceScan = createServiceScan();

        final GenericWorkerApp app = new GenericWorkerApp.Builder(
                "testEnableBindingOverrides", LOGGER)
            .addModule(binder -> binder.bind(MySingletonService.class)
                .toInstance(new MySingletonServiceA()))
            .setServiceScan(serviceScan)
            .addModule(binder -> binder.bind(MySingletonService.class)
                .toInstance(new MySingletonServiceB()))
            .enableBindingOverrides()
            .build();

        // Assert that the binding override uses the service from the second custom module.
        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(MySingletonService.class),
                instanceOf(MySingletonServiceB.class));
    }

    @Test
    public void testAutoBindOverridesCustomBinding() {
        final ServiceScan serviceScan = createServiceScan();

        final GenericWorkerApp app = new GenericWorkerApp.Builder(
                "testEnableBindingOverrides", LOGGER)
                .addModule(binder -> binder.bind(MySingletonService.class)
                        .toInstance(new MySingletonServiceA()))
                .setServiceScan(serviceScan)
                .enableBindingOverrides()
                .build();

        // Assert that the binding override uses the auto-bound service,
        // not the custom module service because the auto-binding came second.
        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(MySingletonService.class),
                instanceOf(MySingleTonServiceAutoBind.class));
    }

    private ServiceScan createServiceScan() {
        final Reflections reflections = mock(Reflections.class);
        when(reflections.getTypesAnnotatedWith(eq(AutoBindSingleton.class)))
            .thenReturn(ImmutableSet.of(MySingleTonServiceAutoBind.class));
        return new ServiceScan.Builder().setReflections(reflections).build();
    }

    private interface MySingletonService {  }

    private class MySingletonServiceA implements MySingletonService {  }

    private class MySingletonServiceB implements MySingletonService {  }

    @AutoBindSingleton(baseClass = MySingletonService.class)
    private static class MySingleTonServiceAutoBind implements MySingletonService {  }
}